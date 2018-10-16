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

import android.text.TextUtils;

import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.netty.Connection;
import org.arpnetwork.arpdevice.netty.Connector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PortProxy extends Connector {
    private static final String TAG = PortProxy.class.getSimpleName();

    private Listener mListener;
    private boolean mTcp;

    public interface Listener {
        void onPort(int port, boolean tcp);

        void onProxy(int port, boolean tcp);

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
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(new String("LISTEN 0\r\n").getBytes());
        conn.write(buf);
    }

    @Override
    public void onChannelRead(Connection conn, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        int len = buf.readableBytes();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        String content = new String(bytes);

        if (!TextUtils.isEmpty(content)) {
            Pattern p = Pattern.compile("(\\d+)");
            Matcher m = p.matcher(content);
            if (m.find()) {
                int port = Integer.parseInt(m.group(1));

                if (mListener != null) {
                    if (content.startsWith("OK")) {
                        mListener.onPort(port, mTcp);
                    } else {
                        mListener.onProxy(port, mTcp);
                    }
                }
            }
        }
    }

    @Override
    public void onException(Connection conn, Throwable cause) {
        super.onException(conn, cause);

        if (mListener != null) {
            mListener.onException(cause);
        }
    }
}
