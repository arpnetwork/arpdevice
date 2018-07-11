/*
 * Copyright 2018 ARP Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.arpnetwork.arpdevice.server;

import android.util.Log;

import org.arpnetwork.arpdevice.data.Message;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.concurrent.GenericFutureListener;

public class NettyConnection {
    private static final int CONNECT_TIMEOUT = 30000;

    private ConnectionHandler mServerHandler;

    private ConnectionListener mListener;
    private int mPort;
    private AtomicInteger mClientNumber = new AtomicInteger(0);

    private EventLoopGroup mWorkerGroup;
    private EventLoopGroup mBossGroup;
    private ChannelFuture mChannelFuture;
    private GenericFutureListener<ChannelFuture> mChannelFutureListener;

    public interface ConnectionListener {
        void onConnected(NettyConnection conn);

        void onClosed(NettyConnection conn);

        void onMessage(NettyConnection conn, Message msg) throws Exception;

        void onException(NettyConnection conn, Throwable cause);
    }

    public NettyConnection(ConnectionListener listener, int port) {
        mListener = listener;
        mPort = port;

        mServerHandler = new ConnectionHandler(this);
    }

    public synchronized void startServer() throws InterruptedException {
        mBossGroup = new NioEventLoopGroup();
        mWorkerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(mBossGroup, mWorkerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            mClientNumber.incrementAndGet();
                            ch.pipeline()
                                    .addLast("decoder", new MessageDecoder())
                                    .addLast("encoder", new MessageEncoder())
                                    .addLast(mServerHandler);
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT);

            mChannelFuture = b.bind(mPort).sync();
            mChannelFuture.channel().closeFuture().sync();

            mChannelFutureListener = new GenericFutureListener<ChannelFuture>() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.cause() != null) {
                        mListener.onException(NettyConnection.this, future.cause());
                    }
                }
            };
            mChannelFuture.addListener(mChannelFutureListener);
        } finally {
            mWorkerGroup.shutdownGracefully();
            mBossGroup.shutdownGracefully();
        }
    }

    private void decrementClient() {
        mClientNumber.decrementAndGet();
    }

    public int getClientNumber() {
        return mClientNumber.intValue();
    }

    public synchronized void shutdown() {
        mChannelFuture.removeListener(mChannelFutureListener);
        try {
            mChannelFuture.sync().channel().close().sync();
        } catch (InterruptedException ignored) {
        }
        mWorkerGroup.shutdownGracefully();
        mBossGroup.shutdownGracefully();
    }

    public void closeConnection() {
        if (mServerHandler.getHandlerContext() == null) return;
        mServerHandler.getHandlerContext().close().addListener(ChannelFutureListener.CLOSE);
    }

    public void write(Message msg) {
        if (mServerHandler.getHandlerContext() == null) return;
        Channel channel = mServerHandler.getHandlerContext().channel();

        channel.writeAndFlush(msg);
    }

    @Sharable
    private static class ConnectionHandler extends ChannelInboundHandlerAdapter {

        private WeakReference<NettyConnection> mConn;

        private ChannelHandlerContext mHandlerContext;

        public ConnectionHandler(NettyConnection conn) {
            mConn = new WeakReference<>(conn);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            NettyConnection conn = mConn.get();
            if (conn != null) {
                if (conn.getClientNumber() > 1) {
                    // only one client can be served and reject the others
                    ctx.close().addListener(ChannelFutureListener.CLOSE);
                    return;
                }
                conn.mListener.onConnected(conn);

                mHandlerContext = ctx;
            }

            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Log.d("NettyConnection", "channelInactive");
            NettyConnection conn = mConn.get();
            if (conn != null) {
                conn.decrementClient();
                conn.mListener.onClosed(conn);
            }

            super.channelInactive(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            NettyConnection conn = mConn.get();
            if (conn != null) {
                conn.mListener.onMessage(conn, (Message) msg);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            Log.d("NettyConnection", "exceptionCaught = " + cause.getMessage());
            NettyConnection conn = mConn.get();
            if (conn != null) {
                conn.decrementClient();
                conn.mListener.onException(conn, cause);
                ctx.close();
                mHandlerContext = null;
            }
        }

        public ChannelHandlerContext getHandlerContext() {
            return mHandlerContext;
        }
    }

    private static class MessageDecoder extends ReplayingDecoder<Void> {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            Message msg = Message.readFrom(in);
            if (msg == null) return;

            out.add(msg);
        }
    }

    private static class MessageEncoder extends MessageToByteEncoder<Message> {
        @Override
        protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) {
            msg.writeTo(out);
        }
    }
}
