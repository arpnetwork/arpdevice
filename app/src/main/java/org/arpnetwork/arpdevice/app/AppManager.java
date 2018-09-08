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

import android.os.Environment;
import android.os.Handler;

import org.arpnetwork.adb.ShellChannel;
import org.arpnetwork.arpdevice.data.DApp;
import org.arpnetwork.arpdevice.device.Adb;
import org.arpnetwork.arpdevice.device.TaskHelper;
import org.arpnetwork.arpdevice.download.DownloadManager;
import org.arpnetwork.arpdevice.download.IDownloadListener;
import org.arpnetwork.arpdevice.server.DataServer;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.util.Util;
import org.web3j.utils.Numeric;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class AppManager {
    private static final String TAG = AppManager.class.getSimpleName();

    private static final int SUCCESS = 0;
    private static final int DOWNLOAD_FAILED = 1;
    private static final int INSTALL_FAILED = 2;

    private static final int TIME_INTERVAL = 2000;

    private DApp mDApp;
    private Set<String> mPackageSet;
    private TaskHelper mTaskHelper;
    private Handler mHandler;
    private State mState;

    public enum State {
        IDLE,
        DOWNLOADING,
        INSTALLING,
        INSTALLED,
        LAUNCHING,
        LAUNCHED
    }

    public AppManager(Handler handler, TaskHelper helper) {
        if (helper == null) {
            throw new IllegalArgumentException("TaskHelper is null");
        }
        mHandler = handler;
        mTaskHelper = helper;
        mPackageSet = new HashSet<>();
        mState = State.IDLE;
    }

    public State getState() {
        return mState;
    }

    public synchronized void setDApp(DApp dApp) {
        mDApp = dApp;
    }

    public DApp getDApp() {
        return mDApp;
    }

    public void appInstall(final String packageName, String url, int fileSize, String md5) {
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
        if (mPackageSet.contains(packageName)) {
            mState = State.LAUNCHING;

            boolean success = mTaskHelper.launchApp(packageName, new Runnable() {
                @Override
                public void run() {
                    mState = State.LAUNCHED;
                    mHandler.sendEmptyMessage(DataServer.MSG_LAUNCH_APP_SUCCESS);
                    mHandler.sendEmptyMessageDelayed(DataServer.MSG_CONNECTED_TIMEOUT, DataServer.CONNECTED_TIMEOUT);
                }
            });
            if (!success) {
                mState = State.INSTALLED;
                mHandler.sendEmptyMessage(DataServer.MSG_LAUNCH_APP_FAILED);
            }
        }
    }

    public void stopApp() {
        mState = State.IDLE;
        mTaskHelper.killLaunchedApp();
    }

    public void uninstallApp(String packageName) {
        mState = State.IDLE;

        Adb adb = new Adb(Touch.getInstance().getConnection());
        adb.uninstallApp(packageName, null);

        File apkFile = new File(Environment.getExternalStorageDirectory(), String.format("arpdevice/%s.apk", packageName));
        if (apkFile.exists()) {
            apkFile.delete();
        }
    }

    public void clear() {
        mState = State.IDLE;
        uninstallAll();
        mPackageSet.clear();
        mDApp = null;
    }

    private synchronized void appInstall(File file, final String packageName) {
        mState = State.INSTALLING;
        mTaskHelper.startCheckTopTimer(TIME_INTERVAL, TIME_INTERVAL);

        Adb adb = new Adb(Touch.getInstance().getConnection());
        adb.installApp(file.getAbsolutePath(), new ShellChannel.ShellListener() {
            @Override
            public void onStdout(ShellChannel ch, byte[] data) {
                mState = State.INSTALLED;
                mPackageSet.add(packageName);
                mTaskHelper.stopCheckTopTimer();
                if (mDApp != null) {
                    DAppApi.appInstalled(packageName, SUCCESS, mDApp);
                }
            }

            @Override
            public void onStderr(ShellChannel ch, byte[] data) {
                mState = State.IDLE;
                mTaskHelper.stopCheckTopTimer();
                if (mDApp != null) {
                    DAppApi.appInstalled(packageName, INSTALL_FAILED, mDApp);
                }
            }

            @Override
            public void onExit(ShellChannel ch, int code) {
            }
        });
    }

    private void uninstallAll() {
        for (String pkg : mPackageSet) {
            uninstallApp(pkg);
        }
    }
}
