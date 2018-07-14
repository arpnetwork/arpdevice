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

import org.arpnetwork.arpdevice.data.DeviceState;
import org.arpnetwork.arpdevice.data.StateChangedResponse;
import org.arpnetwork.arpdevice.data.Message;
import org.arpnetwork.arpdevice.data.NotifyReq;
import org.arpnetwork.arpdevice.data.RegisterReq;
import org.arpnetwork.arpdevice.data.Response;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DeviceManager implements DeviceConnection.Listener {
    private static final String TAG = DeviceManager.class.getSimpleName();

    public static final int REGISTER_SUCCESS = 2;
    public static final int STATE_CHANGED = 3;
    public static final int CONNECTED = 4;
    public static final int CONNECT_TIMEOUT = 5;
    public static final int FINISHED = 6;

    private static final String HOST = "dev.arpnetwork.org";
    private static final int PORT = 8000;
    private static final int HEARTBEAT_INTERVAL = 30000;

    private DeviceConnection mConnection;
    private Gson mGson;

    private String mSession;
    private boolean mRegistered;

    private Handler mHandler = new Handler();
    private OnStateChangedListener mOnStateChangedListener;

    public interface OnStateChangedListener {
        void onStateChanged(int state);
    }

    public DeviceManager() {
        mGson = new Gson();
        mRegistered = false;
    }

    public void setOnStateChangedListener(OnStateChangedListener listener) {
        mOnStateChangedListener = listener;
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
     * @param reqId  {@link #CONNECTED}, {@link #CONNECT_TIMEOUT}, {@link #FINISHED}
     * @param result 0 represents success, 1 represents failed
     */
    public void notifyServer(int reqId, int result) {
        if (mRegistered) {
            NotifyReq req = new NotifyReq(reqId, result, mSession);
            String content = mGson.toJson(req);
            ByteBuf byteBuf = Unpooled.buffer(content.length());
            byteBuf.writeBytes(content.getBytes());

            mConnection.write(new Message(byteBuf));
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
            if (response.id == REGISTER_SUCCESS && response.result == 0) {
                mRegistered = true;
            } else if (response.id == STATE_CHANGED) {
                StateChangedResponse res = mGson.fromJson(json, StateChangedResponse.class);
                if (res.data.state == DeviceState.IDLE) {
                    mSession = null;
                } else if (res.data.state == DeviceState.REQUESTING) {
                    mSession = res.data.session;
                }

                if (mOnStateChangedListener != null) {
                    mOnStateChangedListener.onStateChanged(res.data.state);
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
