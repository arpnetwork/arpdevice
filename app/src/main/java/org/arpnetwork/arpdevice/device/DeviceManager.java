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

import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.data.StateChangedResponse;
import org.arpnetwork.arpdevice.data.Message;
import org.arpnetwork.arpdevice.data.NotifyReq;
import org.arpnetwork.arpdevice.data.RegisterReq;
import org.arpnetwork.arpdevice.data.Response;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DeviceManager implements DeviceConnection.Listener {
    private static final String TAG = DeviceManager.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

    private static final String HOST = "dev.arpnetwork.org";
    private static final int PORT = 8000;
    private static final int HEARTBEAT_INTERVAL = 30000;

    private static DeviceManager sInstance;

    private DeviceConnection mConnection;
    private Gson mGson;

    private String mSession;
    private boolean mRegister;

    private Handler mHandler = new Handler();
    private OnStateChangedListener mOnStateChangedListener;

    public interface OnStateChangedListener {
        void onStateChanged(int state);
    }

    public static DeviceManager getInstance() {
        if (sInstance == null) {
            synchronized (DeviceManager.class) {
                if (sInstance == null) {
                    sInstance = new DeviceManager();
                }
            }
        }
        return sInstance;
    }

    public void setOnStateChangedListener(OnStateChangedListener listener) {
        mOnStateChangedListener = listener;
    }

    public void connect() {
        mConnection = new DeviceConnection(this);
        mConnection.connect(HOST, PORT);
    }

    public void close() {
        if (mConnection != null) {
            mConnection.close();
        }
    }

    public boolean isRegister() {
        return mRegister;
    }

    public String getSession() {
        return mSession;
    }

    public void notifyServer(int reqId, int result) {
        if (mRegister) {
            NotifyReq req = new NotifyReq(reqId, result, mSession);
            String content = mGson.toJson(req);
            if (DEBUG) {
                Log.d(TAG, "notifyServer. req = " + content);
            }
            ByteBuf byteBuf = Unpooled.buffer(content.length());
            byteBuf.writeBytes(content.getBytes());

            mConnection.write(new Message(byteBuf));
        }
    }

    @Override
    public void onConnected(DeviceConnection conn) {
        if (DEBUG) {
            Log.d(TAG, "onConnected");
        }

        register();
        startHeartbeat();
    }

    @Override
    public void onMessage(DeviceConnection conn, Message msg) {
        String json = msg.toJson();
        if (DEBUG) {
            Log.d(TAG, "onMessage. json = " + json);
        }

        if (!TextUtils.isEmpty(json)) {
            Response response = mGson.fromJson(json, Response.class);
            if (response.id == 2 && response.result == 0) {
                mRegister = true;
            } else if (response.id == 3) {
                StateChangedResponse res = mGson.fromJson(json, StateChangedResponse.class);
                if (res.data.state == 0) {
                    mSession = null;
                } else if (res.data.state == 1) {
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
        if (DEBUG) {
            Log.d(TAG, "onClosed");
        }

        reset();
    }

    @Override
    public void onException(DeviceConnection conn, Throwable cause) {
        if (DEBUG) {
            Log.d(TAG, "onException. cause = " + cause);
        }

        reset();
    }

    private DeviceManager() {
        mGson = new Gson();
        mRegister = false;
    }

    private void register() {
        RegisterReq req = new RegisterReq();
        String content = mGson.toJson(req);
        if (DEBUG) {
            Log.d(TAG, "register. req = " + content);
        }
        ByteBuf byteBuf = Unpooled.buffer(content.length());
        byteBuf.writeBytes(content.getBytes());

        mConnection.write(new Message(byteBuf));
    }

    private void startHeartbeat() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (DEBUG) {
                    Log.d(TAG, "startHeartbeat.");
                }
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
