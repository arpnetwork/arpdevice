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

package org.arpnetwork.arpdevice.stream;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import org.arpnetwork.adb.Auth;
import org.arpnetwork.adb.Connection;
import org.arpnetwork.adb.ShellChannel;
import org.arpnetwork.adb.SyncChannel;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.config.Constant;
import org.arpnetwork.arpdevice.monitor.MonitorTouch;
import org.arpnetwork.arpdevice.server.DataServer;
import org.arpnetwork.arpdevice.util.PreferenceManager;

import java.security.spec.InvalidKeySpecException;

public class Touch {
    private static final String TAG = "Touch";
    private static final boolean DEBUG = Config.DEBUG;
    private static final String KEY = "key";

    public static final int STATE_INIT = -1;
    public static final int STATE_CLOSED = 0;
    public static final int STATE_CONNECTED = 1;

    public static final int RETRY_COUNT = 3;

    private static volatile Touch sInstance = null;

    private Connection mConn;
    private ShellChannel mShell;

    private Auth mAuth;
    private String mBanner;

    private int mState;
    private int mRetryCount;

    private boolean mDoAuth = false;
    private Handler mUIHandler;

    private RecordHelper mRecordHelper;
    private MonitorTouch mMonitor;

    public static Touch getInstance() {
        if (sInstance == null) {
            synchronized (Touch.class) {
                if (sInstance == null) {
                    sInstance = new Touch();
                }
            }
        }
        return sInstance;
    }

    public void connect(Handler handler) {
        if (mConn == null) {
            mConn = new Connection(mAuth, "127.0.0.1", 5555);
        }
        mConn.setListener(new CheckConnectionListener(handler));
        if (getState() == Touch.STATE_CONNECTED || mDoAuth) {
            mConn.close();
        }
        mConn.connect();
    }

    public Connection getConnection() {
        return mConn;
    }

    public String getBanner() {
        return mBanner;
    }

    public void sendTouch(String touchInfo) {
        if (!TextUtils.isEmpty(touchInfo) && getState() == STATE_CONNECTED && mShell != null) {
            mShell.write(touchInfo);
            if (touchInfo.startsWith("m")) {
                mShell.write("w 16\n");
            }

            if (mMonitor != null) {
                mMonitor.enqueueTouch(touchInfo);
            }
        }
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    public void startRecord(int quality) {
        mRecordHelper = new RecordHelper();
        if (getState() == STATE_CONNECTED) {
            mRecordHelper.startRecord(quality, mConn);
            mMonitor = new MonitorTouch();
            mMonitor.startMonitor(mBanner.split(" ")[6]);
        }
    }

    public void stopRecord() {
        if (mRecordHelper != null) {
            mRecordHelper.stopRecord();
            mRecordHelper = null;
        }
        if (mMonitor != null) {
            mMonitor.stopMonitor();
        }
    }

    public boolean isRecording() {
        return mRecordHelper != null && mRecordHelper.isRecording();
    }

    private Touch() {
        mState = STATE_INIT;

        String key = PreferenceManager.getInstance().getString(KEY);
        if (!TextUtils.isEmpty(key)) {
            try {
                mAuth = new Auth(key);
            } catch (InvalidKeySpecException e) {
                Log.e(TAG, "InvalidKeySpecException");
            }
        } else {
            mAuth = new Auth();
            PreferenceManager.getInstance().putString(KEY, mAuth.getPrivateKey());
        }

        mUIHandler = new Handler(Looper.getMainLooper());
    }

    private class CheckConnectionListener implements Connection.ConnectionListener {
        private Handler mCheckHandler;

        private CheckConnectionListener(Handler handler) {
            mCheckHandler = handler;
        }

        @Override
        public void onConnected(Connection conn) {
            if (mDoAuth) {
                connect(mCheckHandler);
                mDoAuth = false;
            } else {
                mState = STATE_CONNECTED;
                openUSBSafeDebug();
            }
        }

        @Override
        public void onClosed(Connection conn) {
            mState = STATE_CLOSED;
            // Reconnect.
            if (mRetryCount < RETRY_COUNT) {
                mRetryCount++;
                connect(mCheckHandler);
            } else {
                mRetryCount = 0;
                DataServer.getInstance().onClientDisconnected();
            }
        }

        @Override
        public void onAuth(Connection conn, String key) {
            conn.auth();
            mDoAuth = true;
            Message message = mCheckHandler.obtainMessage(Constant.ACTION_CHECK_AUTH);
            message.obj = mAuth.getPublicKeyDigest();
            message.sendToTarget();
        }

        @Override
        public void onException(Connection conn, Throwable cause) {
            mState = STATE_CLOSED;
        }

        private void openUSBSafeDebug() {
            ShellChannel ss = mConn.openShell("groups");
            ss.setListener(new ShellChannel.ShellListener() {
                StringBuilder sb = new StringBuilder();
                boolean findInput = false;

                @Override
                public void onStdout(ShellChannel ch, byte[] data) {
                    // Check whether contains input.
                    sb.append(new String(data).trim());
                    if (sb.toString().contains("input")) {
                        if (!findInput) {
                            findInput = true;
                            checkTouch();
                        }
                    } else {
                        mCheckHandler.obtainMessage(Constant.CHECK_ADB_SAFE_FAILED).sendToTarget();
                    }
                }

                @Override
                public void onStderr(ShellChannel ch, byte[] data) {
                }

                @Override
                public void onExit(ShellChannel ch, int code) {
                }
            });
        }

        private void checkTouch() {
            if (AssetCopyHelper.isValidTouchBinary()) {
                startTouch();
            } else {
                SyncChannel ss = mConn.openSync();
                AssetCopyHelper.pushTouch(ss, new AssetCopyHelper.PushCallback() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onComplete(boolean success, Throwable throwable) {
                        if (success) {
                            startTouch();
                        } else {
                            Log.e(TAG, "pushTouch error: " + throwable);
                            handleCopyFailed();
                        }
                    }
                });
            }

            if (!AssetCopyHelper.isValidCapBinary()) {
                SyncChannel capChannel = mConn.openSync();
                AssetCopyHelper.pushCap(capChannel, new AssetCopyHelper.PushCallback() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onComplete(boolean success, Throwable throwable) {
                        if (!success) {
                            Log.e(TAG, "pushCap error: " + throwable);
                            handleCopyFailed();
                        }
                    }
                });
            }
            if (!AssetCopyHelper.isValidCapLib()) {
                SyncChannel capLibChannel = mConn.openSync();
                AssetCopyHelper.pushLibCap(capLibChannel, new AssetCopyHelper.PushCallback() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onComplete(boolean success, Throwable throwable) {
                        if (!success) {
                            Log.e(TAG, "pushLibCap error: " + throwable);
                            handleCopyFailed();
                        }
                    }
                });
            }
        }

        private void startTouch() {
            ShellChannel ss = mConn.openShell("/data/local/tmp/arptouch");
            mShell = ss;
            ss.setListener(new ShellChannel.ShellListener() {
                @Override
                public void onStdout(ShellChannel ch, byte[] data) {
                    // contacts x y pressure major minor\n
                    mBanner = new String(data).trim();

                    Message message;
                    boolean installViaUsb = PreferenceManager.getInstance().getBoolean(Constant.KEY_INSTALL_USB);
                    if (!installViaUsb) {
                        message = mCheckHandler.obtainMessage(Constant.ACTION_CHECK_INSTALL);
                    } else {
                        message = mCheckHandler.obtainMessage(Constant.ACTION_CHECK_UPNP);
                    }
                    message.sendToTarget();
                }

                @Override
                public void onStderr(ShellChannel ch, byte[] data) {
                    mCheckHandler.obtainMessage(Constant.CHECK_TOUCH_FAILED).sendToTarget();
                }

                @Override
                public void onExit(ShellChannel ch, int code) {
                }
            });
        }

        private void handleCopyFailed() {
            mCheckHandler.obtainMessage(Constant.CHECK_TOUCH_COPY_FAILED).sendToTarget();
        }

    }
}
