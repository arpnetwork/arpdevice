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

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Message {
    public static final int HEARTBEAT = -5;
    public static final int VIDEO = 0;
    public static final int AUDIO = 1;
    public static final int TOUCH = 2;
    public static final int PROTOCOL = 3;
    public static final int TIME = 4;

    private int mType = -1;
    private ByteBuf mData;

    public Message() {
        this(null);
    }

    public Message(ByteBuf data) {
        mData = data;
    }

    public int getType() {
        if (mType == -1) {
            mType = mData.readByte();
        }
        return mType;
    }

    public String toJson() {
        if (mData != null) {
            int len = mData.readableBytes();
            byte[] bytes = new byte[len];
            mData.readBytes(bytes);
            return new String(bytes);
        }
        return null;
    }

    public static Message readFrom(ByteBuf buf) throws IOException {
        int size = buf.readInt();
        if (size == 0) {
            int bufferSize = 1;
            ByteBuf byteBuf = Unpooled.buffer(bufferSize);
            byteBuf.writeByte(HEARTBEAT);
            return new Message(byteBuf);
        }

        if (buf.readableBytes() < size) {
            return null;
        }

        ByteBuf body = buf.readBytes(size);
        return new Message(body);
    }

    public void writeTo(ByteBuf buf) {
        if (mData == null) {
            buf.writeInt(0); // send heartbeat
        } else {
            buf.writeInt(mData.capacity());
            buf.writeBytes(mData);
        }
    }

    public <T> T parsePacket(Class<T> clazz) {
        Object obj = null;
        T responseT = null;
        switch (mType) {
            case TOUCH:
                int size = mData.readableBytes();
                byte[] bytes = new byte[size];
                mData.readBytes(bytes);
                obj = new String(bytes);
                break;

            case PROTOCOL:
                int allSize = mData.readableBytes();
                byte[] allBytes = new byte[allSize];
                mData.readBytes(allBytes);
                obj = new String(allBytes);
                Gson gson = new Gson();
                return gson.fromJson((String) obj, clazz);

            case TIME:
                obj = mData.readLong();
                break;

            default:
                break;
        }

        try {
            responseT = clazz.cast(obj);
        } catch (ClassCastException e) {
            // silent
        }

        return responseT;
    }
}
