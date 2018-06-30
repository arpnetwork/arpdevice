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

package org.arpnetwork.arpdevice.data;

import com.google.gson.Gson;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ProtocolPacket<T> {
    public static final int CONNECT_REQ = 1;
    public static final int CONNECT_RESP = 2;
    public static final int CHANGE_QUALITY_REQ = 3;
    public static final int CHANGE_QUALITY_RESP = 4;
    public static final int DISCONNECT_REQ = 5;
    public static final int DISCONNECT_RESP = 6;
    public static final int TOUCH_INFO = 100;
    public static final int VIDEO_CHANGED = 101;

    public int id;
    public T data;
    public int result;

    public ProtocolPacket(int id, int result, T data) {
        this.id = id;
        this.data = data;
        this.result = result;
    }

    public static <T> Message generateProtocol(int id, int result, T data) {
        ProtocolPacket<T> packet = new ProtocolPacket<>(id, result, data);
        Gson gson = new Gson();
        String payload = gson.toJson(packet);

        int bufferSize = payload.length() + 1;
        ByteBuf byteBuf = Unpooled.buffer(bufferSize);
        byteBuf.writeByte(Message.PROTOCOL); //type
        byteBuf.writeBytes(payload.getBytes());

        return new Message(byteBuf);
    }

    public static Message generateHeartbeat() {
        return new Message(null);
    }
}
