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

package org.arpnetwork.arpdevice.netty;

import java.util.ArrayList;
import java.util.List;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GenericFutureListener;

public class Connection {
    private static final String TAG = Connection.class.getSimpleName();
    private static final int CONNECT_TIMEOUT = 10000;

    private NioEventLoopGroup mBossGroup;
    private EventLoopGroup mWorkerGroup;
    private ChannelFuture mChannelFuture;
    private List<ChannelHandler> mChannelHandlerList;

    private Listener mListener;

    public interface Listener {
        void onConnected(Connection conn);

        void onClosed(Connection conn);

        void onChannelRead(Connection conn, Object msg) throws Exception;

        void onException(Connection conn, Throwable cause);
    }

    public Connection(Listener listener) {
        mListener = listener;
        mChannelHandlerList = new ArrayList<>();
    }

    public void addHandler(ChannelHandler handler) {
        if (!mChannelHandlerList.contains(handler)) {
            mChannelHandlerList.add(handler);
        }
    }

    public void connect(String host, int port) {
        mWorkerGroup = new NioEventLoopGroup();

        Bootstrap b = new Bootstrap();
        b.group(mWorkerGroup)
                .channel(NioSocketChannel.class)
                .handler(mChannelInitializer)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT);

        mChannelFuture = b.connect(host, port);
        mChannelFuture.addListener(mChannelFutureListener);
    }

    public void startServer(int port) throws Exception {
        mBossGroup = new NioEventLoopGroup();
        mWorkerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(mBossGroup, mWorkerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(mChannelInitializer)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT);

        mChannelFuture = b.bind(port).sync();
        mChannelFuture.addListener(mChannelFutureListener);
    }

    public void close() {
        close(false);
    }

    public void close(boolean shutdown) {
        try {
            mChannelFuture.sync().channel().close().sync();
        } catch (Exception e) {
        } finally {
            if (shutdown) {
                shutdown();
            }
        }
    }

    public void shutdown() {
        mChannelFuture.removeListener(mChannelFutureListener);
        try {
            if (mBossGroup != null) {
                mBossGroup.shutdownGracefully();
            }
            mWorkerGroup.shutdownGracefully();
        } catch (Exception e) {
        }
    }

    public void write(Object msg) {
        if (!mChannelFuture.isSuccess()) {
            return;
        }

        try {
            mChannelFuture.channel().writeAndFlush(msg);
        } catch (Exception e) {
        }
    }

    public void onConnected() {
        if (mListener != null) {
            mListener.onConnected(this);
        }
    }

    public void onClosed() {
        if (mListener != null) {
            mListener.onClosed(this);
        }
    }

    public void onChannelRead(Object msg) throws Exception {
        if (mListener != null) {
            mListener.onChannelRead(this, msg);
        }
    }

    public void onException(Throwable cause) {
        if (mListener != null) {
            mListener.onException(this, cause);
        }
    }

    private ChannelInitializer mChannelInitializer = new ChannelInitializer<SocketChannel>() {
        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            for (ChannelHandler handler : mChannelHandlerList) {
                ch.pipeline().addLast(handler);
            }
        }
    };

    private GenericFutureListener<ChannelFuture> mChannelFutureListener = new GenericFutureListener<ChannelFuture>() {
        @Override
        public void operationComplete(ChannelFuture future) {
            Throwable cause = future.cause();
            if (cause != null) {
                mListener.onException(Connection.this, cause);
            }
        }
    };
}
