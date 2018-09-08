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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import org.arpnetwork.adb.ShellChannel;
import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.server.DataServer;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.util.UIHelper;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class TaskHelper {
    private static final String TAG = "TaskHelper";
    private static final boolean DEBUG = Config.DEBUG;
    private static final int CHECK_TOP_INTERVAL = 800; // shell cost 80ms.

    private String mPackageName;
    private final Adb mAdb;
    private Timer mTimer;
    private Context mContext;
    private Runnable mLaunchRunnable;

    public TaskHelper(Context context) {
        mContext = context;
        mAdb = new Adb(Touch.getInstance().getConnection());
    }

    public boolean launchApp(String packageName, Runnable runnable) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }

        stopCheckTopTimer();
        mPackageName = packageName;
        mLaunchRunnable = runnable;
        PackageManager packageManager = CustomApplication.sInstance.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(mPackageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            CustomApplication.sInstance.startActivity(intent);
            startCheckTopTimer(CHECK_TOP_INTERVAL, CHECK_TOP_INTERVAL);
            return true;
        }
        return false;
    }

    public void startCheckTopTimer(long delay, long period) {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getTopTask();
            }
        }, delay, period);
    }

    public void stopCheckTopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    public void killLaunchedApp() {
        stopCheckTopTimer();
        if (mPackageName != null) {
            mAdb.killApp(mPackageName);
            mAdb.clearApplicationUserData(mPackageName);
            mPackageName = null;
        }
    }

    private void getTopTask() {
        mAdb.getTopAndroidTask(new ShellChannel.ShellListener() {
            @Override
            public void onStdout(ShellChannel ch, byte[] data) {
                String topPackage = getTopPackage(new String(data));
                if (mPackageName != null) {
                    if (topPackage.contains(mPackageName)) {
                        if (mLaunchRunnable != null) {
                            mLaunchRunnable.run();
                            mLaunchRunnable = null;
                        }
                    } else if (topPackage.contains("com.miui.wakepath")) {
                        int x = UIHelper.getWidthNoVirtualBar(mContext) * 3 / 4;
                        int y = UIHelper.getHeightNoVirtualBar(mContext) - UIHelper.dip2px(mContext, 40);
                        sendTouch(x, y);
                    } else {
                        DataServer.getInstance().onClientDisconnected();
                        stopCheckTopTimer();
                    }
                } else if (topPackage.contains("com.miui.securitycenter")) {
                    int x = UIHelper.getWidthNoVirtualBar(mContext) / 4;
                    int y = UIHelper.getHeightNoVirtualBar(mContext) - UIHelper.dip2px(mContext, 40);
                    sendTouch(x, y);
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

    private String getTopPackage(String topTaskPackages) {
        String[] lines = topTaskPackages.split("\n");
        String lastLine = lines[lines.length - 1];
        try {
            return lastLine.split(" ")[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            return "";
        }
    }

    private void sendTouch(int x, int y) {
        String downClick = String.format(Locale.US, "d 0 %d %d 50 5 5\nc\n", x, y);
        Touch.getInstance().sendTouch(downClick);
        Touch.getInstance().sendTouch("u 0 \nc\n");
    }
}
