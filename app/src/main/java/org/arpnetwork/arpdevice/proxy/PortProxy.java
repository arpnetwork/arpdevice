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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PortProxy extends Connector {
    private static final String TAG = PortProxy.class.getSimpleName();

    private Listener mListener;
    private int mAcceptPort;
    private boolean mTcp;

    public interface Listener {
        void onPort(int proxyPort, boolean tcp);

        void onHandshake(int acceptPort, byte[] session, boolean tcp);

        void onException(Throwable cause);
    }

    public PortProxy(Listener listener, boolean tcp) {
        super();

        mListener = listener;
        mTcp = tcp;
    }

    public void connect() {
        super.connect(Config.PROXY_HOST, Config.PROXY_PORT);
    }

    @Override
    public void onConnected(Connection conn) {
        super.onConnected(conn);

        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(new byte[]{3, 0, 0, 0});
        conn.write(buf);
    }

    @Override
    public void onChannelRead(Connection conn, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        do {
            buf.markReaderIndex();
            int size = buf.readByte();
            int readableBytes = buf.readableBytes();
            if (readableBytes < size) {
                buf.resetReaderIndex();
                break;
            }

            int id = buf.readByte();
            if (id == 1) {
                int result = buf.readByte();
                int proxyPort = buf.readUnsignedShort();
                mAcceptPort = buf.readUnsignedShort();

                if (mListener != null) {
                    mListener.onPort(proxyPort, mTcp);
                }
            } else if (id == 2) {
                byte[] bytes = new byte[size - 1];
                buf.readBytes(bytes);

                if (mListener != null) {
                    mListener.onHandshake(mAcceptPort, bytes, mTcp);
                }
            }

            buf.discardReadBytes();
        } while (buf.readableBytes() > 1);
    }

    @Override
    public void onException(Connection conn, Throwable cause) {
        super.onException(conn, cause);

        if (mListener != null) {
            mListener.onException(cause);
        }
    }
}
