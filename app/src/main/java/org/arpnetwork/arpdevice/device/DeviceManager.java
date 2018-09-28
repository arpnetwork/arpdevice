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

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.contracts.api.VerifyAPI;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.data.ReleaseDeviceReq;
import org.arpnetwork.arpdevice.data.SpeedResponse;
import org.arpnetwork.arpdevice.data.DApp;
import org.arpnetwork.arpdevice.data.Req;
import org.arpnetwork.arpdevice.data.SpeedReq;
import org.arpnetwork.arpdevice.data.DeviceAssignedResponse;
import org.arpnetwork.arpdevice.data.Message;
import org.arpnetwork.arpdevice.data.RegisterReq;
import org.arpnetwork.arpdevice.data.Response;
import org.arpnetwork.arpdevice.data.VerifyData;
import org.arpnetwork.arpdevice.data.VerifyReq;
import org.arpnetwork.arpdevice.data.VerifyResponse;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.util.SignUtil;
import org.arpnetwork.arpdevice.util.Util;
import org.web3j.utils.Numeric;

import java.security.SignatureException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DeviceManager implements DeviceConnection.Listener {
    private static final String TAG = DeviceManager.class.getSimpleName();

    public static final int VERIFIED = 2;
    public static final int REGISTERED = 4;
    public static final int DEVICE_ASSIGNED = 6;
    public static final int DEVICE_RELEASED = 7;
    public static final int SPEED_RESULT = 9;
    public static final int DEVICE_OFFLINE = 10;

    private static final int MSG_SPEED = 1;
    private static final int MSG_DEVICE = 2;

    private static final int HEARTBEAT_INTERVAL = 30000;

    private DeviceConnection mConnection;
    private Gson mGson;
    private Miner mMiner;
    private DApp mDapp;
    private VerifyData mVerifyData;
    private ByteBuf mSpeedDataBuf;
    private boolean mRegistered;
    private boolean mClosed;

    private Handler mHandler = new Handler();
    private OnDeviceStateChangedListener mOnDeviceStateChangedListener;

    public interface OnDeviceStateChangedListener {
        void onConnected();

        void onDeviceReady();

        void onDeviceAssigned(DApp dApp);

        void onDeviceReleased();

        void onError(int code, int msg);
    }

    public DeviceManager() {
        mGson = new Gson();
        mRegistered = false;
        mClosed = false;
    }

    public void setOnDeviceStateChangedListener(OnDeviceStateChangedListener listener) {
        mOnDeviceStateChangedListener = listener;
    }

    /**
     * Connect to a server
     */
    public void connect(Miner miner) {
        mMiner = miner;
        mConnection = new DeviceConnection(this);
        mConnection.connect(miner.getIpString(), miner.getPortTcpInt());
    }

    /**
     * Close a connection
     */
    public void close() {
        mClosed = true;
        if (mConnection != null) {
            mConnection.close();
        }
    }

    /**
     * Device releases itself
     */
    public void releaseDevice() {
        send(new ReleaseDeviceReq());
    }

    /**
     * Check whether this device is registered
     */
    public boolean isRegistered() {
        return mRegistered;
    }

    public DApp getDapp() {
        return mDapp;
    }

    @Override
    public void onConnected(DeviceConnection conn) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mOnDeviceStateChangedListener != null) {
                    mOnDeviceStateChangedListener.onConnected();
                }
            }
        });
        verify();
    }

    @Override
    public void onMessage(DeviceConnection conn, Message msg) {
        int type = msg.getType();
        if (type == MSG_SPEED) {
            onSpeedMessage(msg);
        } else if (type == MSG_DEVICE) {
            onDeviceMessage(msg);
        }
    }

    @Override
    public void onClosed(DeviceConnection conn) {
        reset();
        if (!mClosed) {
            handleError(0, R.string.connect_miner_failed);
        }
        mClosed = false;
    }

    @Override
    public void onException(DeviceConnection conn, Throwable cause) {
        Log.e(TAG, "onException. message = " + cause.getMessage());

        mClosed = true;
        handleError(0, R.string.connect_miner_failed);
    }

    private void onDeviceMessage(Message msg) {
        String json = msg.toJson();
        if (!TextUtils.isEmpty(json)) {
            Response response = mGson.fromJson(json, Response.class);
            switch (response.id) {
                case VERIFIED:
                    onVerified(response.result, json);
                    break;
                case REGISTERED:
                    onRegister(response.result);
                    break;
                case DEVICE_ASSIGNED:
                    final DeviceAssignedResponse res = mGson.fromJson(json, DeviceAssignedResponse.class);
                    mDapp = res.data;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mOnDeviceStateChangedListener != null) {
                                mOnDeviceStateChangedListener.onDeviceAssigned(mDapp);
                            }
                        }
                    });
                    break;
                case DEVICE_RELEASED:
                    mDapp = null;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mOnDeviceStateChangedListener != null) {
                                mOnDeviceStateChangedListener.onDeviceReleased();
                            }
                        }
                    });
                    break;
                case SPEED_RESULT:
                    SpeedResponse speedResponse = mGson.fromJson(json, SpeedResponse.class);
                    if (speedResponse.data != null && speedResponse.data.getUploadSpeed() >= 2 * 1024 * 1024) {
                        mHandler.post(mDeviceReadyRunnable);
                    } else {
                        handleError(0, R.string.low_upload_speed);
                    }
                    break;
                case DEVICE_OFFLINE:
                    handleError(0, R.string.device_offline);
                    break;
                default:
                    break;
            }
        }
    }

    private void onSpeedMessage(Message msg) {
        mHandler.removeCallbacks(mDeviceReadyRunnable);

        if (msg.getDataLen() == 0) {
            if (mSpeedDataBuf == null) {
                mSpeedDataBuf = Unpooled.buffer(2 * 1024 * 1024);
            } else {
                int len = mSpeedDataBuf.readableBytes();
                byte[] bytes = new byte[len];
                mSpeedDataBuf.readBytes(bytes);
                send(new SpeedReq(bytes));

                mSpeedDataBuf.clear();
                mSpeedDataBuf = null;
                uploadSpeedData();
            }
        } else {
            if (mSpeedDataBuf != null) {
                mSpeedDataBuf.writeBytes(msg.getData());
            }
        }
    }

    private void onVerified(int result, String json) {
        if (result == 0) {
            VerifyResponse res = mGson.fromJson(json, VerifyResponse.class);
            if (res.data != null) {
                String sign = res.data.getSign();
                try {
                    String addr = VerifyAPI.getSignatureAddress(mVerifyData.getSalt(), sign);
                    if (mMiner != null && Numeric.cleanHexPrefix(mMiner.getAddress()).equalsIgnoreCase(addr)) {
                        register();
                        return;
                    }
                } catch (SignatureException e) {
                }
            }
        }
        handleError(result, R.string.verify_failed);
    }

    private void onRegister(int result) {
        if (result == 0) {
            mRegistered = true;
            mHandler.postDelayed(mDeviceReadyRunnable, 500);
        } else if (result == -1) {
            handleError(result, R.string.incompatible_protocol);
        } else {
            handleError(result, R.string.connect_miner_failed);
        }
    }

    private Runnable mDeviceReadyRunnable = new Runnable() {
        @Override
        public void run() {
            startHeartbeat();
            if (mDapp == null) {
                if (mOnDeviceStateChangedListener != null) {
                    mOnDeviceStateChangedListener.onDeviceReady();
                }
            }
        }
    };

    private void handleError(final int result, final int msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mOnDeviceStateChangedListener != null) {
                    mOnDeviceStateChangedListener.onError(result, msg);
                }
                close();
            }
        });
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

    private void verify() {
        String salt = Util.getRandomString(32);
        String sign = SignUtil.sign(salt);
        String promiseJson = Promise.getJson();
        mVerifyData = new VerifyData(salt, sign, !TextUtils.isEmpty(promiseJson) ? promiseJson : null);
        send(new VerifyReq(mVerifyData));
    }

    private void register() {
        send(new RegisterReq());
    }

    private void uploadSpeedData() {
        int len = 10 * 1024 * 1024;
        String str = Util.getRandomString(len);

        send(MSG_SPEED, null);
        send(MSG_SPEED, str);
        send(MSG_SPEED, null);
    }

    private void send(Req req) {
        String content = mGson.toJson(req);
        send(MSG_DEVICE, content);
    }

    private void send(int type, String content) {
        int len = content != null ? content.length() : 0;
        ByteBuf byteBuf = Unpooled.buffer(len + 1);
        byteBuf.writeByte(type);
        if (content != null) {
            byteBuf.writeBytes(content.getBytes());
        }

        mConnection.write(byteBuf);
    }

    private void reset() {
        mRegistered = false;
        mHandler.removeCallbacksAndMessages(null);
        mDapp = null;
    }
}
