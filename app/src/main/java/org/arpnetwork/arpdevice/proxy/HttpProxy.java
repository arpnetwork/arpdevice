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

package org.arpnetwork.arpdevice.proxy;

import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.netty.Connector;
import org.arpnetwork.arpdevice.server.http.Dispatcher;
import org.arpnetwork.arpdevice.server.http.HttpServerHandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class HttpProxy extends Connector implements HttpServerHandler.Listener {
    private Dispatcher mDispatcher;
    private byte[] mSession;

    public HttpProxy(Dispatcher dispatcher, byte[] session) {
        super();

        mSession = session;
        mDispatcher = dispatcher;
    }

    public void connect(int port) {
        super.connect(Config.PROXY_HOST, port);
    }

    @Override
    public void onChannelActive(ChannelHandlerContext ctx) {
        if (mSession != null) {
            ByteBuf buf = Unpooled.buffer();
            buf.writeByte(mSession.length);
            buf.writeBytes(mSession);
            ctx.writeAndFlush(buf);
        }
    }

    @Override
    public void onChannelInactive(ChannelHandlerContext ctx) {
        close(true);
    }

    @Override
    protected void addHandlers(ChannelPipeline pipeline) {
        pipeline.addLast(new HttpResponseEncoderWrapper(new HttpResponseEncoder()));
        pipeline.addLast(new HttpRequestDecoder());
        pipeline.addLast(new HttpServerHandler(mDispatcher, this));
    }

    static class HttpResponseEncoderWrapper extends ChannelOutboundHandlerAdapter {
        private boolean mInit;
        private HttpResponseEncoder mEncoder;

        public HttpResponseEncoderWrapper(HttpResponseEncoder encoder) {
            mEncoder = encoder;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (!mInit) {
                super.write(ctx, msg, promise);
                mInit = true;
            } else {
                mEncoder.write(ctx, msg, promise);
            }
        }
    }
}
