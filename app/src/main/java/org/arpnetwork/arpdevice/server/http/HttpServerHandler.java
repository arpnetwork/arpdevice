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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import io.netty.handler.codec.http.QueryStringDecoder;

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
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) msg;
            mUri = httpRequest.uri();
            mMethod = httpRequest.method();
        }

        if (msg instanceof HttpContent) {
            Request request = new Request();
            request.setMethod(mMethod);
            request.setUri(mUri);

            if (mMethod == HttpMethod.GET) {
                request.setContent(getRequestParams(mUri));
            }

            if (mMethod == HttpMethod.POST) {
                HttpContent httpContent = (HttpContent) msg;
                ByteBuf content = httpContent.content();
                byte[] data = new byte[content.readableBytes()];
                content.readBytes(data);
                content.release();

                request.setContent(new String(data));
            }

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
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    private String getRequestParams(String uri) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        Map<String, List<String>> params = decoder.parameters();
        Iterator<Map.Entry<String, List<String>>> iterator = params.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<String>> entry = iterator.next();
            jsonObject.put(entry.getKey(), entry.getValue().get(0));
        }
        return jsonObject.toString();
    }
}
