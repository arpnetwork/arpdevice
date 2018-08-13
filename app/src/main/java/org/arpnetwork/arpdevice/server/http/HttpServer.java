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

package org.arpnetwork.arpdevice.server.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class HttpServer {
    private int mPort;
    private Dispatcher mDispatcher;
    private NioEventLoopGroup mBossGroup;
    private NioEventLoopGroup mWorkerGroup;
    private ChannelFuture mChannelFuture;

    public HttpServer(int port, Dispatcher dispatcher) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher is null");
        }
        mPort = port;
        mDispatcher = dispatcher;
    }

    public void start() throws Exception {
        mBossGroup = new NioEventLoopGroup();
        mWorkerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(mBossGroup, mWorkerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new HttpServerInitializer(mDispatcher))
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        mChannelFuture = b.bind(mPort).sync();
    }

    public void stop() {
        try {
            mChannelFuture.sync().channel().close().sync();
        } catch (Exception e) {
        }
        mWorkerGroup.shutdownGracefully();
        mBossGroup.shutdownGracefully();
    }
}
