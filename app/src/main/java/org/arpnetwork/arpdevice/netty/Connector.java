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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public abstract class Connector implements Connection.Listener {
    private final String TAG = getClass().getSimpleName();

    private Connection mConnection;

    public Connector() {
    }

    public void resetConnection(Connection conn) {
        mConnection = conn;
    }

    public void connect(String host, int port) {
        initConnection();
        mConnection.connect(host, port);
    }

    public void startServer(int port) throws Exception {
        initConnection();
        mConnection.startServer(port);
    }

    public void write(Object msg) {
        if (mConnection != null) {
            mConnection.write(msg);
        }
    }

    public void closeChannel() {
        if (mConnection != null) {
            mConnection.closeChannel();
        }
    }

    public void close() {
        close(false);
    }

    public void close(boolean shutdown) {
        if (mConnection != null) {
            mConnection.close(shutdown);
        }
    }

    public void shutdown() {
        if (mConnection != null) {
            mConnection.shutdown();
        }
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

    protected void addHandlers(ChannelPipeline pipeline) {
        pipeline.addLast(new ConnectionHandler(mConnection));
    }

    private void initConnection() {
        mConnection = new Connection(this, new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                addHandlers(ch.pipeline());
            }
        });
    }
}