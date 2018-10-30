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
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;

import org.arpnetwork.adb.ShellChannel;
import org.arpnetwork.adb.SyncChannel;
import org.arpnetwork.arpdevice.constant.Constant;
import org.arpnetwork.arpdevice.device.Adb;
import org.arpnetwork.arpdevice.device.TaskHelper;
import org.arpnetwork.arpdevice.stream.AssetCopyHelper;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.util.DeviceUtil;
import org.arpnetwork.arpdevice.util.PreferenceManager;
import org.arpnetwork.arpdevice.util.Util;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class CheckThread {
    private static final String TAG = "CheckThread";
    private static final int PING_INTERVAL = 800;
    private static final long DISK_REQUEST = 1024 * 1024 * 1024;
    private static final int DELAY_START_TIMER = 2000;
    private static final String sCheckerPkgName = "org.arpnetwork.arpchecker";
    private static final String sMd5 = "2e4e4cdd8edfa310228b08b89e256847";

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

        int allowChargingAdb = 1;
        if (Build.MANUFACTURER.equalsIgnoreCase("huawei")) {
            try {
                allowChargingAdb = Settings.Global.getInt(mContext.getContentResolver(), "allow_charging_adb");
            } catch (Settings.SettingNotFoundException ignored) {
                // no allow_charging_adb. we consider allow_charging_adb = 1 as default.
            }
        }
        if (DeviceUtil.getSdk() < Build.VERSION_CODES.N) {
            Message message = mUIHandler.obtainMessage(Constant.CHECK_OS_FAILED);
            mUIHandler.sendMessage(message);
        } else if (DeviceUtil.getExternalDiskAvailable(mContext) < DISK_REQUEST) {
            Message message = mUIHandler.obtainMessage(Constant.CHECK_DISK_FAILED);
            mUIHandler.sendMessage(message);
        } else if (Build.MANUFACTURER.equalsIgnoreCase("huawei") && allowChargingAdb == 0) {
            Message message = mUIHandler.obtainMessage(Constant.CHECK_ADB_ALLOW_CHARGING_FAILED);
            mUIHandler.sendMessage(message);
        } else if (Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 0) {
            Message message = mUIHandler.obtainMessage(Constant.CHECK_ADB_FAILED);
            mUIHandler.sendMessage(message);
        } else if (mShouldPing) {
            startPingTimer();
        }
    }

    public void setShouldPing(boolean should) {
        mShouldPing = should;
    }

    public void turnOnStay() {
        Adb adb = new Adb(Touch.getInstance().getConnection());
        adb.stayOn();
    }

    public void adbInstallConfirmOff() {
        Adb adb = new Adb(Touch.getInstance().getConnection());
        adb.adbInstallConfirmOff();
    }

    public void quit() {
        mThread.quit();
        stopPingTimer();
    }

    public void checkInstall() {
        Touch.getInstance().openTouch(false);
        stopCheckPackageTimer();
        uninstallApp(sCheckerPkgName);

        File destDir = new File(Environment.getExternalStorageDirectory(), "arpdevice");
        if (!destDir.exists()) {
            if (!destDir.mkdirs()) {
                mUIHandler.obtainMessage(Constant.ACTION_CHECK_INSTALL).sendToTarget();
                return;
            }
        }

        boolean apkExists = false;
        final File apkFile = new File(destDir, String.format("%s.apk", sCheckerPkgName));
        if (apkFile.exists() && apkFile.length() > 0) {
            String fileMd5 = Util.md5(apkFile);
            if (fileMd5 != null && fileMd5.equalsIgnoreCase(Numeric.cleanHexPrefix(sMd5))) {
                apkExists = true;
            }
        }

        if (!apkExists) {
            try {
                SyncChannel ss = Touch.getInstance().getConnection().openSync();
                AssetCopyHelper.pushFileFromAsset(ss, apkFile.getCanonicalPath(), "arpchecker.apk", new AssetCopyHelper.PushCallback() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onComplete(boolean success, Throwable throwable) {
                        if (success) {
                            appInstall(apkFile, sCheckerPkgName);
                        } else {
                            mUIHandler.obtainMessage(Constant.ACTION_CHECK_INSTALL).sendToTarget();
                        }
                    }
                });
            } catch (IOException e) {
                mUIHandler.obtainMessage(Constant.ACTION_CHECK_INSTALL).sendToTarget();
            }
        } else {
            appInstall(apkFile, sCheckerPkgName);
        }
    }

    private void stopPingTimer() {
        mWorkerHandler.removeCallbacks(mPingRunnable);
    }

    private void startPingTimer() {
        if (mWorkerHandler != null) {
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

                Touch.getInstance().connect(mUIHandler);
            } else {
                Message message = mUIHandler.obtainMessage(Constant.CHECK_TCP_FAILED);
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

    private void appInstall(File file, final String packageName) {
        final TaskHelper mTaskHelper = new TaskHelper(mContext.getApplicationContext());
        mTaskHelper.startCheckTopTimer(1000, DELAY_START_TIMER);

        Adb adb = new Adb(Touch.getInstance().getConnection());
        adb.installApp(file.getAbsolutePath(), new ShellChannel.ShellListener() {
            @Override
            public void onStdout(ShellChannel ch, byte[] data) {
                mTaskHelper.stopCheckTopTimer();
                startCheckPackageTimer();
            }

            @Override
            public void onStderr(ShellChannel ch, byte[] data) {
                mTaskHelper.stopCheckTopTimer();
                stopCheckPackageTimer();
                mUIHandler.obtainMessage(Constant.CHECK_INSTALL_FAILED).sendToTarget();
            }

            @Override
            public void onExit(ShellChannel ch, int code) {
            }
        });
    }

    private void uninstallApp(String packageName) {
        Adb adb = new Adb(Touch.getInstance().getConnection());
        adb.uninstallApp(packageName, null);
    }

    private void stopCheckPackageTimer() {
        mWorkerHandler.removeCallbacks(mCheckPackageRunnable);
    }

    private void startCheckPackageTimer() {
        if (mWorkerHandler != null) {
            mWorkerHandler.postDelayed(mCheckPackageRunnable, PING_INTERVAL);
        }
    }

    private Runnable mCheckPackageRunnable = new Runnable() {
        @Override
        public void run() {
            boolean hasInstalled = Util.hasPackage(mContext, sCheckerPkgName);

            if (hasInstalled) {
                Touch.getInstance().closeTouch();
                stopCheckPackageTimer();

                Message message = mUIHandler.obtainMessage(Constant.ACTION_CHECK_UPNP);
                mUIHandler.sendMessage(message);

                PreferenceManager.getInstance().putBoolean(Constant.KEY_INSTALL_USB, true);
                uninstallApp(sCheckerPkgName);
            } else {
                mWorkerHandler.postDelayed(this, PING_INTERVAL);
            }
        }
    };
}
