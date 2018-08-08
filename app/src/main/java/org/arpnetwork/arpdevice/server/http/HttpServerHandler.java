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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

class HttpServerHandler extends ChannelInboundHandlerAdapter {
    private static final String TAG = HttpServerHandler.class.getSimpleName();

    private static final String CONTENT_TYPE = "content-type";
    private static final String CONTENT_LENGTH = "content-length";
    private static final String CONNECTION = "connection";

    private Dispatcher mDispatcher;
    private HttpMethod mMethod;
    private String mUri;

    public HttpServerHandler(Dispatcher dispatcher) {
        super();

        mDispatcher = dispatcher;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) msg;
            mUri = httpRequest.uri();
            mMethod = httpRequest.method();
        }

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            ByteBuf content = httpContent.content();
            byte[] data = new byte[content.readableBytes()];
            content.readBytes(data);
            content.release();

            Request request = new Request();
            request.setMethod(mMethod);
            request.setUri(mUri);
            request.setContent(new String(data));

            Response response = new Response();

            mDispatcher.service(request, response);

            FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.valueOf(response.getStatus()), Unpooled.wrappedBuffer(response.getContent().getBytes()));
            httpResponse.headers().set(CONTENT_TYPE, response.getContentType());
            httpResponse.headers().set(CONTENT_LENGTH, httpResponse.content().readableBytes());
            httpResponse.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.write(httpResponse);
            ctx.flush();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
