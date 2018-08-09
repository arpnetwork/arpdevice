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

import org.arpnetwork.arpdevice.contracts.api.VerifyAPI;
import org.arpnetwork.arpdevice.contracts.tasks.BindMinerHelper;
import org.arpnetwork.arpdevice.data.Req;
import org.arpnetwork.arpdevice.data.SpeedReq;
import org.arpnetwork.arpdevice.data.UserRequestResponse;
import org.arpnetwork.arpdevice.data.Message;
import org.arpnetwork.arpdevice.data.RegisterReq;
import org.arpnetwork.arpdevice.data.Response;
import org.arpnetwork.arpdevice.data.VerifyData;
import org.arpnetwork.arpdevice.data.VerifyReq;
import org.arpnetwork.arpdevice.data.VerifyResponse;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
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
    public static final int CLIENT_REQUEST = 6;

    private static final int MSG_SPEED = 1;
    private static final int MSG_DEVICE = 2;

    private static final String HOST = "dev.arpnetwork.org";
    private static final int PORT = 8000;
    private static final int HEARTBEAT_INTERVAL = 30000;

    private DeviceConnection mConnection;
    private Gson mGson;

    private VerifyData mVerifyData;
    private ByteBuf mSpeedDataBuf;
    private String mDappAddress;
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
        return "";
    }

    @Override
    public void onConnected(DeviceConnection conn) {
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
            if (response.id == VERIFIED) {
                if (response.result == 0) {
                    VerifyResponse res = mGson.fromJson(json, VerifyResponse.class);
                    if (res.data != null) {
                        String sign = res.data.getSign();
                        try {
                            String addr = VerifyAPI.getSignatureAddress(mVerifyData.getSalt(), sign);
                            Miner miner = BindMinerHelper.getBinded(Wallet.get().getPublicKey());
                            if (miner != null && Numeric.cleanHexPrefix(miner.address).equalsIgnoreCase(addr)) {
                                register();
                                return;
                            }
                        } catch (SignatureException e) {
                        }
                    }
                    handleError(response.result, "Verify miner failed");
                } else {
                    handleError(response.result, "Verify device failed");
                }
            } else if (response.id == REGISTERED) {
                if (response.result == 0) {
                    mRegistered = true;
                    startHeartbeat();
                } else {
                    handleError(response.result, "Incompatible protocol");
                }
            } else if (response.id == CLIENT_REQUEST) {
                UserRequestResponse res = mGson.fromJson(json, UserRequestResponse.class);
                mDappAddress = res.data.address;

                if (mOnClientRequestListener != null) {
                    mOnClientRequestListener.onClientRequest();
                }
            }
        }
    }

    private void onSpeedMessage(Message msg) {
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

    private void handleError(int result, String msg) {
        close();
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(result, msg);
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

    private void verify() {
        String salt = Util.getRandomString(32);
        String sign = SignUtil.sign(salt);
        mVerifyData = new VerifyData(salt, sign);
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
        mDappAddress = null;
        mHandler.removeCallbacksAndMessages(null);
    }
}
