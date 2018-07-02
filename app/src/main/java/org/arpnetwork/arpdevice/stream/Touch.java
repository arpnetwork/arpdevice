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
import org.arpnetwork.adb.Channel;
import org.arpnetwork.adb.Connection;
import org.arpnetwork.adb.ShellChannel;
import org.arpnetwork.arpdevice.CustomApplication;

import java.security.spec.InvalidKeySpecException;

public class Touch {
    private static final String TAG = "Touch";

    private static volatile Touch sInstance = null;

    private Connection mConn;
    private ShellChannel mShell;

    private Auth mAuth;

    private String mBanner;

    public static Touch getInstance() {
        if (null == sInstance) {
            synchronized (Touch.class) {
                if (null == sInstance) {
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
                ShellChannel ss = mConn.openShell("/data/local/tmp/arptouch");
                mShell = ss;
                ss.setStreamListener(new Channel.ChannelListener() {

                    @Override
                    public void onOpened(Channel ch) {
                    }

                    @Override
                    public void onClosed(Channel ch) {
                    }
                });
                ss.setListener(new ShellChannel.ShellListener() {

                    @Override
                    public void onStdout(ShellChannel ch, String data) {
                        // 10 1440 2560 0 255 255\n
                        mBanner = data.trim();
                    }

                    @Override
                    public void onStderr(ShellChannel ch, String data) {
                        Log.e(TAG, "STDERR:" + data);
                    }

                    @Override
                    public void onExit(ShellChannel ch, int code) {
                        Log.i(TAG, "Exit with code = " + code);
                    }
                });
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

    private Touch() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(CustomApplication.sInstance.getApplicationContext());
        String key = sp.getString("key", null);
        if (key != null) {
            try {
                mAuth = new Auth(key);
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            }
        } else {
            mAuth = new Auth();
            sp.edit().putString("key", mAuth.getPrivateKey()).apply();
        }
    }
}
