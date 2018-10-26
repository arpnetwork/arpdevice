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
import org.arpnetwork.arpdevice.data.Message;
import org.arpnetwork.arpdevice.netty.Connection;
import org.arpnetwork.arpdevice.netty.DefaultConnector;
import org.arpnetwork.arpdevice.server.DataServer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TcpProxy extends DefaultConnector {
    private byte[] mSession;

    public TcpProxy(byte[] session) {
        super();

        mSession = session;
    }

    public void connect(int port) {
        connect(Config.PROXY_HOST, port);
    }

    @Override
    public void onConnected(Connection conn) {
        if (mSession != null) {
            ByteBuf buf = Unpooled.buffer();
            buf.writeByte(mSession.length);
            buf.writeBytes(mSession);
            write(buf);
        }

        DataServer.getInstance().resetConnection(conn);
        DataServer.getInstance().onConnected(conn);
    }

    @Override
    protected void onMessage(Connection conn, Message msg) throws Exception {
        DataServer.getInstance().onMessage(conn, msg);
    }

    @Override
    public void onClosed(Connection conn) {
        DataServer.getInstance().onClosed(conn);
        close(true);
    }

    @Override
    public void onException(Connection conn, Throwable cause) {
        DataServer.getInstance().onException(conn, cause);
    }
}
