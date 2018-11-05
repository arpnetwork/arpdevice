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

import org.arpnetwork.arpdevice.constant.ErrorCode;
import org.arpnetwork.arpdevice.data.AppInfo;
import org.arpnetwork.arpdevice.data.AppInfoResponse;
import org.arpnetwork.arpdevice.data.NetType;
import org.arpnetwork.arpdevice.data.PromiseResponse;
import org.arpnetwork.arpdevice.data.RegisterResponse;

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
import org.arpnetwork.arpdevice.netty.Connection;
import org.arpnetwork.arpdevice.netty.DefaultConnector;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.util.SignUtil;
import org.arpnetwork.arpdevice.util.Util;
import org.web3j.utils.Numeric;

import java.security.SignatureException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DeviceManager extends DefaultConnector {
    private static final String TAG = DeviceManager.class.getSimpleName();

    private static final int VERIFIED = 2;
    private static final int REGISTERED = 4;
    private static final int DEVICE_ASSIGNED = 6;
    private static final int DEVICE_RELEASED = 7;
    private static final int SPEED_RESULT = 9;
    private static final int DEVICE_OFFLINE = 10;
    private static final int RECEIVE_PROMISE = 11;
    private static final int APP_INSTALL = 12;
    private static final int APP_UNINSTALL = 13;
    private static final int APP_START = 14;

    private static final int LOW_SPEED = -6;
    private static final int INCOMPATIBLE_PROTOCOL = -1;
    private static final int SUCCESS = 0;

    private static final int MSG_SPEED = 1;
    private static final int MSG_DEVICE = 2;

    private static final int HEARTBEAT_INTERVAL = 30000;

    private Gson mGson;
    private Miner mMiner;
    private DApp mDapp;
    private VerifyData mVerifyData;
    private ByteBuf mSpeedDataBuf;
    private boolean mClosed;
    private boolean mException;

    private Handler mHandler;
    private OnDeviceStateChangedListener mOnDeviceListener;

    public interface OnDeviceStateChangedListener {
        void onConnected();

        void onDeviceReady();

        void onDeviceAssigned(DApp dApp);

        void onDeviceReleased();

        void onPromiseReceived(Promise promise);

        void onAppInstall(AppInfo info);

        void onAppUninstall(String pkgName);

        void onAppStart(String pkgName);

        void onError(int code, int msg);
    }

    public DeviceManager() {
        super();

        mGson = new Gson();
        mHandler = new Handler();
    }

    public void setOnDeviceStateChangedListener(OnDeviceStateChangedListener listener) {
        mOnDeviceListener = listener;
    }

    /**
     * Connect to a miner
     */
    public void connect(Miner miner) {
        mClosed = false;
        mException = false;
        mMiner = miner;
        connect(miner.getIpString(), miner.getPortTcpInt());
    }

    /**
     * Close a connection
     */
    public void close() {
        mClosed = true;
        close(true);
    }

    /**
     * Device releases itself
     */
    public void releaseDevice() {
        send(new ReleaseDeviceReq());
    }

    public DApp getDapp() {
        return mDapp;
    }

    @Override
    public void onConnected(Connection conn) {
        super.onConnected(conn);

        mHandler.post(mConnectedRunnable);
        verify();
    }

    @Override
    public void onMessage(Connection conn, Message msg) {
        int type = msg.getType();
        if (type == MSG_SPEED) {
            onSpeedMessage(msg);
        } else if (type == MSG_DEVICE) {
            onDeviceMessage(msg);
        }
    }

    @Override
    public void onClosed(Connection conn) {
        if (!mException) {
            reset();
            if (!mClosed) {
                handleError(ErrorCode.RESET_BY_REMOTE, R.string.connect_miner_failed);
            }
            mClosed = false;
        }
    }

    @Override
    public void onException(Connection conn, Throwable cause) {
        mException = true;
        reset();
        handleError(ErrorCode.NETWORK_ERROR, R.string.connect_miner_failed);
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
                    onRegister(response.result, json);
                    break;

                case DEVICE_ASSIGNED:
                    DeviceAssignedResponse res = mGson.fromJson(json, DeviceAssignedResponse.class);
                    mDapp = res.data;
                    mHandler.post(mDeviceAssignRunnable);
                    break;

                case DEVICE_RELEASED:
                    mDapp = null;
                    mHandler.post(mDeviceReleasedRunnable);
                    break;

                case SPEED_RESULT:
                    SpeedResponse speedResponse = mGson.fromJson(json, SpeedResponse.class);
                    if (speedResponse.result == 0) {
                        mHandler.post(mDeviceReadyRunnable);
                    } else if (speedResponse.result == LOW_SPEED) {
                        handleError(speedResponse.result, R.string.low_upload_speed);
                    } else {
                        handleError(speedResponse.result, R.string.speed_test_failed);
                    }
                    break;

                case DEVICE_OFFLINE:
                    handleError(0, R.string.device_offline);
                    break;

                case RECEIVE_PROMISE:
                    onPromiseReceived(json);
                    break;

                case APP_INSTALL:
                    onAppInstall(json);
                    break;

                case APP_UNINSTALL:
                    onAppUninstall(json);
                    break;

                case APP_START:
                    onAppStart(json);
                    break;

                default:
                    break;
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

    private void onRegister(int result, String json) {
        switch (result) {
            case SUCCESS:
                RegisterResponse res = mGson.fromJson(json, RegisterResponse.class);
                //FIXME Show the type of network to user
                if (res.data.netType == NetType.EXTRANET) {
                } else {
                }
                break;
            case INCOMPATIBLE_PROTOCOL:
                handleError(result, R.string.incompatible_protocol);
                break;
            default:
                handleError(result, R.string.connect_miner_failed);
                break;
        }
    }

    private void onPromiseReceived(String json) {
        final PromiseResponse res = mGson.fromJson(json, PromiseResponse.class);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mOnDeviceListener != null) {
                    mOnDeviceListener.onPromiseReceived(res.data);
                }
            }
        });
    }

    private void onAppInstall(String json) {
        final AppInfoResponse res = mGson.fromJson(json, AppInfoResponse.class);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mOnDeviceListener != null) {
                    mOnDeviceListener.onAppInstall(res.data);
                }
            }
        });
    }

    private void onAppUninstall(String json) {
        final AppInfoResponse res = mGson.fromJson(json, AppInfoResponse.class);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mOnDeviceListener != null) {
                    mOnDeviceListener.onAppUninstall(res.data.packageName);
                }
            }
        });
    }

    private void onAppStart(String json) {
        final AppInfoResponse res = mGson.fromJson(json, AppInfoResponse.class);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mOnDeviceListener != null) {
                    mOnDeviceListener.onAppStart(res.data.packageName);
                }
            }
        });
    }

    private void handleError(final int result, final int msg) {
        mClosed = true;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mOnDeviceListener != null) {
                    mOnDeviceListener.onError(result, msg);
                }
                close();
            }
        });
    }

    private void startHeartbeat() {
        mHandler.postDelayed(mHeartbeatRunnable, HEARTBEAT_INTERVAL);
    }

    private void verify() {
        String salt = Util.getRandomString(32);
        String sign = SignUtil.signWithAccount(salt);
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

        write(new Message(byteBuf));
    }

    private void reset() {
        mHandler.removeCallbacks(mConnectedRunnable);
        mHandler.removeCallbacks(mDeviceReadyRunnable);
        mHandler.removeCallbacks(mDeviceAssignRunnable);
        mHandler.removeCallbacks(mDeviceReleasedRunnable);
        mHandler.removeCallbacks(mHeartbeatRunnable);
        mDapp = null;
    }

    private Runnable mConnectedRunnable = new Runnable() {
        @Override
        public void run() {
            if (mOnDeviceListener != null) {
                mOnDeviceListener.onConnected();
            }
        }
    };

    private Runnable mDeviceReadyRunnable = new Runnable() {
        @Override
        public void run() {
            startHeartbeat();
            if (mDapp == null) {
                if (mOnDeviceListener != null) {
                    mOnDeviceListener.onDeviceReady();
                }
            }
        }
    };

    private Runnable mDeviceAssignRunnable = new Runnable() {
        @Override
        public void run() {
            if (mOnDeviceListener != null) {
                mOnDeviceListener.onDeviceAssigned(mDapp);
            }
        }
    };

    private Runnable mDeviceReleasedRunnable = new Runnable() {
        @Override
        public void run() {
            if (mOnDeviceListener != null) {
                mOnDeviceListener.onDeviceReleased();
            }
        }
    };

    private Runnable mHeartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            write(new Message());
            startHeartbeat();
        }
    };
}
