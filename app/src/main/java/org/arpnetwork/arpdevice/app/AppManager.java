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

package org.arpnetwork.arpdevice.app;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;

import org.arpnetwork.adb.ShellChannel;
import org.arpnetwork.arpdevice.data.DApp;
import org.arpnetwork.arpdevice.database.InstalledApp;
import org.arpnetwork.arpdevice.device.Adb;
import org.arpnetwork.arpdevice.device.TaskHelper;
import org.arpnetwork.arpdevice.download.DownloadManager;
import org.arpnetwork.arpdevice.download.IDownloadListener;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.util.Util;
import org.web3j.utils.Numeric;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class AppManager {
    private static final String TAG = AppManager.class.getSimpleName();

    private static final int LAUNCH_TIMEOUT = 10000;

    private static final int INSTALL_SUCCESS = 0;
    private static final int DOWNLOAD_FAILED = 1;
    private static final int INSTALL_FAILED = 2;

    private static final int DELAY_START_TIMER = 2000;
    private static final int DELAY_REMOVE_APP = 5 * 60 * 1000;

    private static final int SCREEN_BRIGHTNESS_RUNNING = 4;

    private static AppManager sInstance;

    private DApp mDApp;
    private String mLastDAppAddress;
    private List<String> mInstalledApps;
    private TaskHelper mTaskHelper;
    private Handler mHandler;
    private State mState;
    private OnAppManagerListener mOnAppManagerListener;

    private int mScreenBrightnessMode = -1; // 0: close 1:open
    private int mScreenBrightness = -1; // 0-255

    public enum State {
        IDLE,
        DOWNLOADING,
        INSTALLING,
        INSTALLED,
        LAUNCHING,
        LAUNCHED
    }

    public interface OnAppManagerListener {
        void onAppInstall(boolean success);

        void onAppLaunch(boolean success);
    }

    public static AppManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AppManager(context);
        }
        return sInstance;
    }

    public void setOnAppManagerListener(OnAppManagerListener listener) {
        mOnAppManagerListener = listener;
    }

    public void setOnTopTaskListener(TaskHelper.OnTopTaskListener listener) {
        mTaskHelper.setOnTopTaskListener(listener);
    }

    public void getTopTask(TaskHelper.OnGetTopTaskListener listener) {
        mTaskHelper.getTopTask(listener);
    }

    public State getState() {
        return mState;
    }

    public synchronized void setDApp(DApp dApp) {
        mHandler.removeCallbacksAndMessages(null);
        if (mLastDAppAddress != null) {
            if (!mLastDAppAddress.equals(dApp.address)) {
                uninstallAll();
            }
        }
        mDApp = dApp;
        mLastDAppAddress = dApp.address;

        if (dApp != null) {
            getInstalledApps();
        }
    }

    public DApp getDApp() {
        return mDApp;
    }

    public void appInstall(final String packageName, String url, int fileSize, String md5) {
        boolean isInstalled = mInstalledApps != null && mInstalledApps.contains(packageName);
        if (isInstalled) {
            mTaskHelper.clearUserData(packageName);
            if (!InstalledApp.exists(packageName)) {
                saveInstalledApp(packageName);
            }
            DAppApi.appInstalled(packageName, INSTALL_SUCCESS, mDApp);
            mState = State.INSTALLED;
            return;
        }
        if (mState == State.LAUNCHING || mState == State.LAUNCHED) {
            DAppApi.appInstalled(packageName, INSTALL_FAILED, mDApp);
            return;
        }

        File destDir = new File(Environment.getExternalStorageDirectory(), "arpdevice");
        if (!destDir.exists()) {
            if (!destDir.mkdirs()) {
                DAppApi.appInstalled(packageName, DOWNLOAD_FAILED, mDApp);
                return;
            }
        }

        boolean apkExists = false;
        File apkFile = new File(destDir, String.format("%s.apk", packageName));
        if (apkFile.exists() && apkFile.length() > 0) {
            String fileMd5 = Util.md5(apkFile);
            if (fileMd5 != null && fileMd5.equalsIgnoreCase(Numeric.cleanHexPrefix(md5))) {
                apkExists = true;
            }
        }

        if (!apkExists) {
            mState = State.DOWNLOADING;
            DownloadManager.getInstance().start(url, apkFile, new IDownloadListener() {
                @Override
                public void onFinish(File file) {
                    appInstall(file, packageName);
                }

                @Override
                public void onError(Exception e) {
                    if (mDApp != null) {
                        DAppApi.appInstalled(packageName, DOWNLOAD_FAILED, mDApp);
                    }
                }
            });
        } else {
            appInstall(apkFile, packageName);
        }
    }

    public void startApp(final String pkgName) {
        if (InstalledApp.exists(pkgName)) {
            mState = State.LAUNCHING;

            postLaunchFailedDelayed();
            boolean success = mTaskHelper.launchApp(pkgName, new Runnable() {
                @Override
                public void run() {
                    mState = State.LAUNCHED;
                    onAppLaunch(true);

                    globalDimOn(SCREEN_BRIGHTNESS_RUNNING); // dim screen
                }
            });
            if (!success) {
                mState = State.INSTALLED;
                onAppLaunch(false);
            }
        } else {
            mState = State.IDLE;
            onAppLaunch(false);
        }
    }

    public void stopApp() {
        mState = State.IDLE;
        mHandler.removeCallbacks(mLaunchFailedRunnable);
        mTaskHelper.killLaunchedApp();

        globalDimOff(); // restore bright
    }

    public void uninstallApp(String packageName) {
        mState = State.IDLE;
        mTaskHelper.uninstallApp(packageName, null);

        File apkFile = new File(Environment.getExternalStorageDirectory(), String.format("arpdevice/%s.apk", packageName));
        if (apkFile.exists()) {
            apkFile.delete();
        }

        InstalledApp.delete(packageName);
    }

    public void clear() {
        mState = State.IDLE;
        mDApp = null;
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mLastDAppAddress = null;
                uninstallAll();
            }
        }, DELAY_REMOVE_APP);
    }

    private AppManager(Context context) {
        mTaskHelper = new TaskHelper(context.getApplicationContext());
        mHandler = new Handler();
        mState = State.IDLE;
    }

    private void getInstalledApps() {
        mTaskHelper.getInstalledApps(new TaskHelper.OnGetInstalledAppsListener() {
            @Override
            public void onGetInstalledApps(List<String> apps) {
                mInstalledApps = apps;
            }
        });
    }

    private void saveInstalledApp(String pkgName) {
        InstalledApp installedApp = new InstalledApp();
        installedApp.pkgName = pkgName;
        installedApp.saveRecord();

    }

    private synchronized void appInstall(File file, final String packageName) {
        mState = State.INSTALLING;
        mTaskHelper.startCheckTopTimer(DELAY_START_TIMER, DELAY_START_TIMER);
        mTaskHelper.installApp(file.getAbsolutePath(), new ShellChannel.ShellListener() {
            @Override
            public void onStdout(ShellChannel ch, byte[] data) {
                mState = State.INSTALLED;
                saveInstalledApp(packageName);

                mTaskHelper.stopCheckTopTimer();
                if (mDApp != null) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            DAppApi.appInstalled(packageName, INSTALL_SUCCESS, mDApp);
                        }
                    }, 1000);
                    if (mOnAppManagerListener != null) {
                        mOnAppManagerListener.onAppInstall(true);
                    }
                }
            }

            @Override
            public void onStderr(ShellChannel ch, byte[] data) {
                mState = State.IDLE;
                mTaskHelper.stopCheckTopTimer();
                if (mDApp != null) {
                    DAppApi.appInstalled(packageName, INSTALL_FAILED, mDApp);
                    if (mOnAppManagerListener != null) {
                        mOnAppManagerListener.onAppInstall(false);
                    }
                }
            }

            @Override
            public void onExit(ShellChannel ch, int code) {
            }
        });
    }

    private void uninstallAll() {
        List<InstalledApp> list = InstalledApp.findAll();
        for (InstalledApp app : list) {
            uninstallApp(app.pkgName);
        }
    }

    private void postLaunchFailedDelayed() {
        mHandler.postDelayed(mLaunchFailedRunnable, LAUNCH_TIMEOUT);
    }

    private void onAppLaunch(boolean success) {
        mHandler.removeCallbacks(mLaunchFailedRunnable);
        if (mOnAppManagerListener != null) {
            mOnAppManagerListener.onAppLaunch(success);
        }
    }

    private void globalDimOn(int screenBrightness) {
        Adb adb = new Adb(Touch.getInstance().getConnection());
        final LinkedList<String> items = new LinkedList<String>();
        adb.globalDimOn(screenBrightness, new ShellChannel.ShellListener() {
            @Override
            public void onStdout(ShellChannel ch, byte[] data) {
                // 1
                // 89
                String item = new String(data).trim();
                items.add(item);
                if (items.size() > 1) {
                    mScreenBrightnessMode = Integer.parseInt(items.get(0).trim());
                    mScreenBrightness = Integer.parseInt(items.get(1).trim());
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

    private void globalDimOff() {
        Adb adb = new Adb(Touch.getInstance().getConnection());
        adb.globalDimRestore(mScreenBrightnessMode, mScreenBrightness);
    }

    private Runnable mLaunchFailedRunnable = new Runnable() {
        @Override
        public void run() {
            onAppLaunch(false);
        }
    };
}
