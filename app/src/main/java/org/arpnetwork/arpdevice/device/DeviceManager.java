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

import org.arpnetwork.arpdevice.data.UserRequestResponse;
import org.arpnetwork.arpdevice.data.Message;
import org.arpnetwork.arpdevice.data.NotifyReq;
import org.arpnetwork.arpdevice.data.RegisterReq;
import org.arpnetwork.arpdevice.data.Response;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DeviceManager implements DeviceConnection.Listener {
    private static final String TAG = DeviceManager.class.getSimpleName();

    public static final int REGISTERED = 2;
    public static final int CLIENT_REQUEST = 3;
    public static final int CONNECTED = 4;
    public static final int CONNECTED_TIMEOUT = 5;
    public static final int FINISHED = 6;

    private static final String HOST = "dev.arpnetwork.org";
    private static final int PORT = 8000;
    private static final int HEARTBEAT_INTERVAL = 30000;

    private DeviceConnection mConnection;
    private Gson mGson;

    private String mSession;
    private boolean mRegistered;

    private Handler mHandler = new Handler();
    private OnClientRequestListener mOnClientRequestListener;

    public interface OnClientRequestListener {
        void onClientRequest();
    }

    public DeviceManager() {
        mGson = new Gson();
        mRegistered = false;
    }

    public void setOnClientRequestListener(OnClientRequestListener listener) {
        mOnClientRequestListener = listener;
    }

    /**
     * Connect to a server
     */
    public void connect() {
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
            NotifyReq req = new NotifyReq(reqId);
            String content = mGson.toJson(req);
            ByteBuf byteBuf = Unpooled.buffer(content.length());
            byteBuf.writeBytes(content.getBytes());

            mConnection.write(new Message(byteBuf));

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
        String json = msg.toJson();
        if (!TextUtils.isEmpty(json)) {
            Response response = mGson.fromJson(json, Response.class);
            if (response.id == REGISTERED && response.result == 0) {
                mRegistered = true;
            } else if (response.id == CLIENT_REQUEST) {
                UserRequestResponse res = mGson.fromJson(json, UserRequestResponse.class);
                mSession = res.data.session;

                if (mOnClientRequestListener != null) {
                    mOnClientRequestListener.onClientRequest();
                }
            }
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

    private void register() {
        RegisterReq req = new RegisterReq();
        String content = mGson.toJson(req);
        ByteBuf byteBuf = Unpooled.buffer(content.length());
        byteBuf.writeBytes(content.getBytes());

        mConnection.write(new Message(byteBuf));
    }

    private void startHeartbeat() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mConnection.write(new Message());
                startHeartbeat();
            }
        }, HEARTBEAT_INTERVAL);
    }

    private void reset() {
        mSession = null;
        mHandler.removeCallbacksAndMessages(null);
    }
}
