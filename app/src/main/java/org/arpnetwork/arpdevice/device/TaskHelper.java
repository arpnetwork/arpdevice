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

import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import org.arpnetwork.adb.ShellChannel;
import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.server.DataServer;
import org.arpnetwork.arpdevice.stream.Touch;

import java.util.Timer;
import java.util.TimerTask;

public class TaskHelper {
    private static final String TAG = "TaskHelper";
    private static final boolean DEBUG = Config.DEBUG;
    private static final int CHECK_TOP_INTERVAL = 800; // shell cost 80ms.

    private String mPackageName;
    private final Adb mAdb;
    private Timer mTimer;

    public TaskHelper() {
        mTimer = new Timer();
        mAdb = new Adb(Touch.getInstance().getConnection());
    }

    public boolean launchApp(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }

        mPackageName = packageName;
        PackageManager packageManager = CustomApplication.sInstance.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(mPackageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            CustomApplication.sInstance.startActivity(intent);
            startCheckTopTimer();
            return true;
        }
        return false;
    }

    private void startCheckTopTimer() {
        mTimer.schedule(mTimerTask, CHECK_TOP_INTERVAL, CHECK_TOP_INTERVAL);
    }

    private TimerTask mTimerTask = new TimerTask() {
        @Override
        public void run() {
            mAdb.getTopAndroidTask(new ShellChannel.ShellListener() {
                @Override
                public void onStdout(ShellChannel ch, byte[] data) {
                    String topPackage = getTopPackage(new String(data));
                    if (!topPackage.contains(mPackageName)) {
                        DataServer.getInstance().onClientDisconnected();
                        mTimer.cancel();
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
    };

    public void killLaunchedApp() {
        mTimer.cancel();
        mAdb.killApp(mPackageName);
        mAdb.clearApplicationUserData(mPackageName);
    }

    private String getTopPackage(String topTaskPackages) {
        String[] lines = topTaskPackages.split("\n");
        String lastLine = lines[lines.length - 1];
        try {
            return lastLine.split(" ")[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            return "";
        }
    }
}
