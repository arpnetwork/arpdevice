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

package org.arpnetwork.arpdevice.ui;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;

import org.arpnetwork.arpdevice.config.Constant;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.util.DeviceUtil;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class CheckThread {
    private static final int PING_INTERVAL = 800;
    private static final long DISK_REQUEST = 1024 * 1024 * 1024;

    private final Context mContext;
    private final HandlerThread mThread;
    private final Handler mWorkerHandler;
    private final Handler mUIHandler;

    private boolean mShouldPing = true;

    public CheckThread(Context context, Handler uiHandler) {
        mContext = context;
        mThread = new HandlerThread("CheckDeviceHandlerThread");
        mThread.start();
        mWorkerHandler = new Handler(mThread.getLooper());
        mUIHandler = uiHandler;
    }

    public void doCheck() {
        stopPingTimer();

        if (DeviceUtil.getSdk() < Build.VERSION_CODES.N) {
            Message message = mUIHandler.obtainMessage(Constant.CHECK_OS);
            mUIHandler.sendMessage(message);
        } else if (DeviceUtil.getExternalDiskAvailable(mContext) < DISK_REQUEST) {
            Message message = mUIHandler.obtainMessage(Constant.CHECK_DISK_AVAILABLE);
            mUIHandler.sendMessage(message);
        } else if (Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 0) {
            Message message = mUIHandler.obtainMessage(Constant.CHECK_ADB);
            mUIHandler.sendMessage(message);
        } else if (Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0) == 0) {
            // TODO：turn on stay on while plugged in by code
        } else if (mShouldPing) {
            startPingTimer();
        }
    }

    public void setShouldPing(boolean should) {
        mShouldPing = should;
    }

    public void quit() {
        mThread.quit();
        
        stopPingTimer();
    }

    private void stopPingTimer() {
        mWorkerHandler.removeCallbacksAndMessages(null);
    }

    private void startPingTimer() {
        if (mWorkerHandler != null) {
            mWorkerHandler.removeCallbacksAndMessages(null);
            mWorkerHandler.postDelayed(mPingRunnable, PING_INTERVAL);
        }
    }

    private Runnable mPingRunnable = new Runnable() {
        @Override
        public void run() {
            boolean opened = isPortInOpened("127.0.0.1", 5555);
            mShouldPing = !opened;

            if (opened) {
                stopPingTimer();

                Touch.getInstance().ensureAuthChecked(mUIHandler);
            } else {
                Message message = mUIHandler.obtainMessage(Constant.CHECK_TCP);
                mUIHandler.sendMessage(message);

                mWorkerHandler.postDelayed(this, PING_INTERVAL);
            }
        }
    };

    private boolean isPortInOpened(String host, int port) {
        // Assume no connection is possible.
        boolean result = false;

        try {
            new Socket(host, port).close();
            result = true;
        } catch (SocketException e) {
            // Could not connect.
        } catch (IOException e) {
            // Could not connect.
        }

        return result;
    }
}
