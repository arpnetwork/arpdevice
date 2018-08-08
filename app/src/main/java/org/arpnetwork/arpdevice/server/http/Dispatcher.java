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

import io.netty.handler.codec.http.HttpMethod;

public abstract class Dispatcher {
    private static final String TAG = Dispatcher.class.getSimpleName();

    public Dispatcher() {
    }

    public final void service(Request request, Response response) {
        response.setStatus(200);
        response.setContent(request.getContent());
        response.setContentType("text/json;charset=utf-8");

        if (request.getMethod() == HttpMethod.GET) {
            onGet(request, response);
        }

        if (request.getMethod() == HttpMethod.POST) {
            onPost(request, response);
        }
    }

    protected abstract void onGet(Request request, Response response);

    protected abstract void onPost(Request request, Response response);
}
