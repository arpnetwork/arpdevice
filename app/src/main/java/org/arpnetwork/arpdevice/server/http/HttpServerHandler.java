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

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
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
    private ConcurrentMap<ChannelId, RequestData> mRequestDataMap;

    public HttpServerHandler(Dispatcher dispatcher) {
        super();

        mDispatcher = dispatcher;
        mRequestDataMap = new ConcurrentHashMap<>();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ChannelId channelId = ctx.channel().id();
        RequestData requestData = mRequestDataMap.get(channelId);
        if (requestData == null) {
            requestData = new RequestData();
            requestData.strBuffer = new StringBuffer();
            mRequestDataMap.put(channelId, requestData);
        }

        if (msg instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) msg;
            requestData.uri = httpRequest.uri();
            requestData.method = httpRequest.method();
        }

        if (msg instanceof HttpContent) {
            if (ctx.channel().remoteAddress() instanceof InetSocketAddress) {
                InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                requestData.remoteAddress = socketAddress.getAddress().getHostAddress();
            }

            if (requestData.method == HttpMethod.GET) {
                String content = getRequestParams(requestData.uri);
                requestData.strBuffer.append(content);
            }

            if (requestData.method == HttpMethod.POST) {
                ByteBuf buf = ((HttpContent) msg).content();
                byte[] data = new byte[buf.readableBytes()];
                buf.readBytes(data);
                buf.release();

                String content = new String(data);
                requestData.strBuffer.append(content);
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ChannelId channelId = ctx.channel().id();
        RequestData requestData = mRequestDataMap.get(channelId);
        mRequestDataMap.remove(channelId);

        Request request = new Request();
        request.setMethod(requestData.method);
        request.setUri(requestData.uri);
        request.setRemoteAddress(requestData.remoteAddress);
        request.setContent(requestData.strBuffer.toString());

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

    private class RequestData {
        HttpMethod method;
        String uri;
        String remoteAddress;
        StringBuffer strBuffer;
    }
}
