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

package org.arpnetwork.arpdevice.device;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import org.arpnetwork.arpdevice.data.Req;
import org.arpnetwork.arpdevice.data.UploadSpeedReq;
import org.arpnetwork.arpdevice.data.UserRequestResponse;
import org.arpnetwork.arpdevice.data.Message;
import org.arpnetwork.arpdevice.data.NotifyReq;
import org.arpnetwork.arpdevice.data.RegisterReq;
import org.arpnetwork.arpdevice.data.Response;
import org.arpnetwork.arpdevice.util.Util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DeviceManager implements DeviceConnection.Listener {
    private static final String TAG = DeviceManager.class.getSimpleName();

    public static final int REGISTERED = 2;
    public static final int CLIENT_REQUEST = 3;
    public static final int CONNECTED = 4;
    public static final int CONNECTED_TIMEOUT = 5;
    public static final int FINISHED = 6;

    private static final int MSG_DEVICE = 1;
    private static final int MSG_SPEED = 2;

    private static final int FLAG_DATA = 1;
    private static final int FLAG_START = 2;
    private static final int FLAG_END = 3;

    private static final String HOST = "dev.arpnetwork.org";
    private static final int PORT = 8000;
    private static final int HEARTBEAT_INTERVAL = 30000;

    private DeviceConnection mConnection;
    private Gson mGson;

    private String mSession;
    private long mCurrentTime;
    private int mDataLen;
    private boolean mRegistered;

    private Handler mHandler = new Handler();
    private OnClientRequestListener mOnClientRequestListener;
    private OnErrorListener mOnErrorListener;

    public interface OnClientRequestListener {
        void onClientRequest();
    }

    public interface OnErrorListener {
        void onError(int code, String msg);
    }

    public DeviceManager() {
        mGson = new Gson();
        mRegistered = false;
    }

    public void setOnClientRequestListener(OnClientRequestListener listener) {
        mOnClientRequestListener = listener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    /**
     * Connect to a server
     */
    public void connect() {
        mDataLen = 0;
        mConnection = new DeviceConnection(this);
        mConnection.connect(HOST, PORT);
    }

    /**
     * Close a connection
     */
    public void close() {
        if (mConnection != null) {
            mConnection.close();
        }
    }

    /**
     * Check whether this device is registered
     */
    public boolean isRegistered() {
        return mRegistered;
    }

    /**
     * Return the session which is assigned from the server
     */
    public String getSession() {
        return mSession;
    }

    /**
     * Notify the server when this device's state is changed
     *
     * @param reqId {@link #CONNECTED}, {@link #CONNECTED_TIMEOUT}, {@link #FINISHED}
     */
    public void notifyServer(int reqId) {
        if (mRegistered) {
            send(new NotifyReq(reqId));

            if (reqId == FINISHED) {
                mSession = null;
            }
        }
    }

    @Override
    public void onConnected(DeviceConnection conn) {
        register();
        startHeartbeat();
    }

    @Override
    public void onMessage(DeviceConnection conn, Message msg) {
        int type = msg.getType();
        if (type == MSG_DEVICE) {
            onDeviceMessage(msg);
        } else if (type == MSG_SPEED) {
            onSpeedMessage(msg);
        }
    }

    @Override
    public void onClosed(DeviceConnection conn) {
        reset();
    }

    @Override
    public void onException(DeviceConnection conn, Throwable cause) {
        Log.e(TAG, "onException. message = " + cause.getMessage());

        reset();
    }

    private void onDeviceMessage(Message msg) {
        String json = msg.toJson();
        if (!TextUtils.isEmpty(json)) {
            Response response = mGson.fromJson(json, Response.class);
            if (response.id == REGISTERED) {
                if (response.result == 0) {
                    mRegistered = true;
                } else {
                    if (mOnErrorListener != null) {
                        mOnErrorListener.onError(response.result, "Incompatible protocol");
                    }
                }
            } else if (response.id == CLIENT_REQUEST) {
                UserRequestResponse res = mGson.fromJson(json, UserRequestResponse.class);
                mSession = res.data.session;

                if (mOnClientRequestListener != null) {
                    mOnClientRequestListener.onClientRequest();
                }
            }
        }
    }

    private void onSpeedMessage(Message msg) {
        int flag = msg.getData().readByte();
        switch (flag) {
            case FLAG_START:
                mCurrentTime = System.currentTimeMillis();
                mDataLen = 0;
                break;

            case FLAG_DATA:
                mDataLen += msg.getDataLen();
                break;

            case FLAG_END:
                float interval = (System.currentTimeMillis() - mCurrentTime) / 1000.0f;
                int speed = (int) (mDataLen / interval);
                uploadSpeed(speed);
                sendRandomData();
                break;

            default:
                break;
        }
    }

    private void startHeartbeat() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mConnection.write(null);
                startHeartbeat();
            }
        }, HEARTBEAT_INTERVAL);
    }

    private void register() {
        send(new RegisterReq());
    }

    private void uploadSpeed(int speed) {
        send(new UploadSpeedReq(speed));
    }

    private void sendRandomData() {
        int len = 10 * 1024 * 1024;
        String str = Util.getRandomString(len);

        sendFlagData(FLAG_START);
        send(MSG_SPEED, FLAG_DATA, str);
        sendFlagData(FLAG_END);
    }

    private void sendFlagData(int flag) {
        send(MSG_SPEED, flag, null);
    }

    private void send(Req req) {
        String content = mGson.toJson(req);
        send(MSG_DEVICE, -1, content);
    }

    private void send(int type, int flag, String content) {
        int len = content != null ? content.length() : 0;
        if (type == MSG_DEVICE) {
            len += 1;
        } else {
            len += 2;
        }
        ByteBuf byteBuf = Unpooled.buffer(len);
        byteBuf.writeByte(type);
        if (type == MSG_SPEED) {
            byteBuf.writeByte(flag);
        }
        if (content != null) {
            byteBuf.writeBytes(content.getBytes());
        }

        mConnection.write(byteBuf);
    }

    private void reset() {
        mSession = null;
        mHandler.removeCallbacksAndMessages(null);
    }
}
