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
import org.arpnetwork.arpdevice.device.TaskHelper;
import org.arpnetwork.arpdevice.download.DownloadManager;
import org.arpnetwork.arpdevice.download.IDownloadListener;
import org.arpnetwork.arpdevice.server.DataServer;
import org.arpnetwork.arpdevice.util.Util;
import org.web3j.utils.Numeric;

import java.io.File;
import java.util.List;

public class AppManager {
    private static final String TAG = AppManager.class.getSimpleName();

    private static final int LAUNCH_TIMEOUT = 10000;

    private static final int INSTALL_SUCCESS = 0;
    private static final int DOWNLOAD_FAILED = 1;
    private static final int INSTALL_FAILED = 2;

    private static final int DELAY_START_TIMER = 2000;
    private static final int DELAY_REMOVE_APP = 5 * 60 * 1000;

    private static AppManager sInstance;

    private DApp mDApp;
    private String mLastDAppAddress;
    private List<String> mInstalledApps;
    private TaskHelper mTaskHelper;
    private Handler mOuterHandler;
    private Handler mInnerHandler;
    private State mState;
    private OnAppManagerListener mOnAppManagerListener;

    public enum State {
        IDLE,
        DOWNLOADING,
        INSTALLING,
        INSTALLED,
        LAUNCHING,
        LAUNCHED
    }

    public interface OnAppManagerListener {
        void onInstall(boolean success);
    }

    public static AppManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AppManager(context);
        }
        return sInstance;
    }

    public void setHandler(Handler handler) {
        mOuterHandler = handler;
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

    public void getInstalledApps() {
        mTaskHelper.getInstalledApps(new TaskHelper.OnGetInstalledAppsListener() {
            @Override
            public void onGetInstalledApps(List<String> apps) {
                mInstalledApps = apps;
            }
        });
    }

    public State getState() {
        return mState;
    }

    public synchronized void setDApp(DApp dApp) {
        if (mLastDAppAddress != null) {
            mInnerHandler.removeCallbacksAndMessages(null);
            if (!mLastDAppAddress.equals(dApp.address)) {
                uninstallAll();
            }
        }
        mDApp = dApp;
        mLastDAppAddress = dApp.address;
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

    public void startApp(String packageName) {
        if (InstalledApp.exists(packageName)) {
            mState = State.LAUNCHING;

            if (mOuterHandler != null) {
                mOuterHandler.sendEmptyMessageDelayed(DataServer.MSG_LAUNCH_APP_FAILED, LAUNCH_TIMEOUT);
            }
            boolean success = mTaskHelper.launchApp(packageName, new Runnable() {
                @Override
                public void run() {
                    mState = State.LAUNCHED;
                    if (mOuterHandler != null) {
                        mOuterHandler.removeMessages(DataServer.MSG_LAUNCH_APP_FAILED);
                        mOuterHandler.sendEmptyMessage(DataServer.MSG_LAUNCH_APP_SUCCESS);
                    }
                }
            });
            if (!success) {
                mState = State.INSTALLED;
                if (mOuterHandler != null) {
                    mOuterHandler.removeMessages(DataServer.MSG_LAUNCH_APP_FAILED);
                    mOuterHandler.sendEmptyMessage(DataServer.MSG_LAUNCH_APP_FAILED);
                }
            }
        } else {
            mState = State.IDLE;
            if (mOuterHandler != null) {
                mOuterHandler.sendEmptyMessage(DataServer.MSG_LAUNCH_APP_FAILED);
            }
        }
    }

    public void stopApp() {
        mState = State.IDLE;
        mTaskHelper.killLaunchedApp();
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
        mInnerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mLastDAppAddress = null;
                uninstallAll();
            }
        }, DELAY_REMOVE_APP);
    }

    private AppManager(Context context) {
        mTaskHelper = new TaskHelper(context.getApplicationContext());
        mInnerHandler = new Handler();
        mState = State.IDLE;
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
                    DAppApi.appInstalled(packageName, INSTALL_SUCCESS, mDApp);
                    if (mOnAppManagerListener != null) {
                        mOnAppManagerListener.onInstall(true);
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
                        mOnAppManagerListener.onInstall(false);
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
}
