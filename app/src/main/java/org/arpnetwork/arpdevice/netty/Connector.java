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

import io.netty.channel.ChannelHandler;

public abstract class Connector implements Connection.Listener {
    private static final String TAG = Connector.class.getSimpleName();

    private Connection mConnection;

    public Connector() {
        mConnection = new Connection(this);
        addHandlers();
    }

    public void resetConnection(Connection conn) {
        mConnection = conn;
    }

    public void connect(String host, int port) {
        mConnection.connect(host, port);
    }

    public void startServer(int port) throws Exception {
        mConnection.startServer(port);
    }

    public void write(Object msg) {
        mConnection.write(msg);
    }

    public void close() {
        close(false);
    }

    public void close(boolean shutdown) {
        mConnection.close(shutdown);
    }

    public void shutdown() {
        mConnection.shutdown();
    }

    @Override
    public void onConnected(Connection conn) {
    }

    @Override
    public void onChannelRead(Connection conn, Object msg) throws Exception {
    }

    @Override
    public void onClosed(Connection conn) {
    }

    @Override
    public void onException(Connection conn, Throwable cause) {
    }

    protected void addHandlers() {
        addHandler(new ConnectionHandler(mConnection));
    }

    protected void addHandler(ChannelHandler handler) {
        mConnection.addHandler(handler);
    }
}