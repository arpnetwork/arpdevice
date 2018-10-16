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
import org.arpnetwork.arpdevice.netty.Connection;
import org.arpnetwork.arpdevice.netty.Connector;
import org.arpnetwork.arpdevice.server.http.Dispatcher;
import org.arpnetwork.arpdevice.server.http.HttpServerHandler;

import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class HttpProxy extends Connector {

    public HttpProxy(Dispatcher dispatcher) {
        super();

        addHandler(new HttpServerHandler(dispatcher));
    }

    public void connect(int port) {
        super.connect(Config.PROXY_HOST, port);
    }

    @Override
    public void onClosed(Connection conn) {
        super.onClosed(conn);

        shutdown();
    }

    @Override
    protected void addHandlers() {
        addHandler(new HttpResponseEncoder());
        addHandler(new HttpRequestDecoder());
    }
}
