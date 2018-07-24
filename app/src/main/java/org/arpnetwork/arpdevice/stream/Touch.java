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

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import org.arpnetwork.adb.Auth;
import org.arpnetwork.adb.Connection;
import org.arpnetwork.adb.ShellChannel;
import org.arpnetwork.adb.SyncChannel;
import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.server.DataServer;

import java.security.spec.InvalidKeySpecException;

public class Touch {
    private static final String TAG = "Touch";
    private static final boolean DEBUG = Config.DEBUG;

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

    private RecordHelper mRecordHelper;

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

    public void connect() {
        if (mConn == null) {
            mConn = new Connection(mAuth, "127.0.0.1", 5555);
            mConn.setListener(mConnectionListener);
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
        }
    }

    public void stopRecord(){
        if (mRecordHelper != null){
            mRecordHelper.stopRecord();
            mRecordHelper = null;
        }
    }

    public boolean isRecording(){
        return mRecordHelper != null && mRecordHelper.isRecording();
    }

    private void startTouch() {
        ShellChannel ss = mConn.openShell("/data/local/tmp/arptouch");
        mShell = ss;
        ss.setListener(new ShellChannel.ShellListener() {
            @Override
            public void onStdout(ShellChannel ch, byte[] data) {
                // contacts x y pressure major minor\n
                mBanner = new String(data).trim();
            }

            @Override
            public void onStderr(ShellChannel ch, byte[] data) {
                Log.e(TAG, "STDERR:" + data);
            }

            @Override
            public void onExit(ShellChannel ch, int code) {
                Log.e(TAG, "onExit:" + code);
            }
        });
    }

    private Touch() {
        mState = STATE_INIT;

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(CustomApplication.sInstance.getApplicationContext());
        String key = sp.getString("key", null);
        if (key != null) {
            try {
                mAuth = new Auth(key);
            } catch (InvalidKeySpecException e) {
                Log.e(TAG, "InvalidKeySpecException");
            }
        } else {
            mAuth = new Auth();
            sp.edit().putString("key", mAuth.getPrivateKey()).apply();
        }
    }

    private Connection.ConnectionListener mConnectionListener = new Connection.ConnectionListener() {
        @Override
        public void onConnected(Connection conn) {
            mState = STATE_CONNECTED;
            mRetryCount = 0;

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
                            Log.e(TAG, "push error: " + throwable);
                        }
                    }
                });
            }

            if (!AssetCopyHelper.isValidCapBinary()) {
                SyncChannel capChannel = mConn.openSync();
                AssetCopyHelper.pushCap(capChannel, null);
            }
            if (!AssetCopyHelper.isValidCapLib()) {
                SyncChannel capLibChannel = mConn.openSync();
                AssetCopyHelper.pushLibCap(capLibChannel,null);
            }
        }

        @Override
        public void onClosed(Connection conn) {
            mState = STATE_CLOSED;
            // Reconnect.
            if (mRetryCount < RETRY_COUNT) {
                mRetryCount++;
                connect();
            } else {
                mRetryCount = 0;
                DataServer.getInstance().onClientDisconnected();
            }
        }

        @Override
        public void onAuth(Connection conn, String key) {
        }

        @Override
        public void onException(Connection conn, Throwable cause) {
            Log.e(TAG, "EXCEPTION: " + cause.getMessage());
            mState = STATE_CLOSED;
        }
    };
}
