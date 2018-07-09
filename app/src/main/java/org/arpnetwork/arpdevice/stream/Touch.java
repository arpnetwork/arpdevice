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
import org.arpnetwork.arpdevice.CustomApplication;

import java.security.spec.InvalidKeySpecException;

public class Touch {
    private static final String TAG = "Touch";
    private static final boolean DEBUG = false;

    private static volatile Touch sInstance = null;

    private Connection mConn;
    private ShellChannel mShell;

    private Auth mAuth;
    private String mBanner;

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
        mConn = new Connection(mAuth, "127.0.0.1", 5555);
        mConn.setListener(new Connection.ConnectionListener() {
            @Override
            public void onConnected(Connection conn) {
                if (AssetCopyHelper.isValidTouchBinary()) {
                    startTouch();
                } else {
                    if (AssetCopyHelper.copyTouch2Sdcard()){
                        doCopyWithStart();
                    } else {
                        Log.e(TAG, "copy to sdcard failed.");
                    }
                }
            }

            @Override
            public void onClosed(Connection conn) {
                Log.d(TAG, "closed.");
            }

            @Override
            public void onAuth(Connection conn, String key) {
            }

            @Override
            public void onException(Connection conn, Throwable cause) {
                Log.e(TAG, "EXCEPTION: " + cause.getMessage());
            }
        });

        mConn.connect();
    }

    public String getBanner() {
        return mBanner;
    }

    public void sendTouch(String touchInfo) {
        if (!TextUtils.isEmpty(touchInfo) && mShell != null) {
            mShell.write(touchInfo);
        }
    }

    private void startTouch() {
        ShellChannel ss = mConn.openShell("/data/local/tmp/arptouch");
        mShell = ss;
        ss.setListener(new SimpleShellListener() {
            @Override
            public void onStdout(ShellChannel ch, String data) {
                // contacts x y pressure major minor\n
                mBanner = data.trim();
            }

            @Override
            public void onStderr(ShellChannel ch, String data) {
                Log.e(TAG, "STDERR:" + data);
            }
        });
    }

    private void doCopyWithStart() {
        // Use chmod 777 to make arptouch executable and we can calculate its md5.
        ShellChannel ss = mConn.openShell("cp sdcard/arptouch /data/local/tmp/; chmod 777 /data/local/tmp/arptouch");
        ss.setListener(new SimpleShellListener() {
            @Override
            public void onExit(ShellChannel ch, int code) {
                startTouch();
            }
        });
    }

    private Touch() {
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

    private class SimpleShellListener implements ShellChannel.ShellListener {
        @Override
        public void onStdout(ShellChannel ch, String data) {
            // Empty implementation.
        }

        @Override
        public void onStderr(ShellChannel ch, String data) {
            // Empty implementation.
        }

        @Override
        public void onExit(ShellChannel ch, int code) {
            // Empty implementation.
        }
    }
}
