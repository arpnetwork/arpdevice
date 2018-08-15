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
import android.util.Log;

import org.arpnetwork.adb.ShellChannel;
import org.arpnetwork.arpdevice.data.DApp;
import org.arpnetwork.arpdevice.device.Adb;
import org.arpnetwork.arpdevice.device.TaskHelper;
import org.arpnetwork.arpdevice.download.DownloadManager;
import org.arpnetwork.arpdevice.download.IDownloadListener;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.util.Util;
import org.web3j.utils.Numeric;

import java.io.File;

public class AppManager {
    private static final String TAG = AppManager.class.getSimpleName();

    private static final int SUCCESS = 0;
    private static final int DOWNLOAD_FAILED = 1;
    private static final int INSTALL_FAILED = 2;

    private TaskHelper mTaskHelper;
    private DApp mDApp;

    public AppManager() {
        mTaskHelper = new TaskHelper();
    }

    public void setDApp(DApp dApp) {
        mDApp = dApp;
    }

    public void appInstall(Context context, final String packageName, String url, int fileSize, String md5) {
        File destDir = context.getFilesDir();
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
            DownloadManager.getInstance().start(url, apkFile, new IDownloadListener() {
                @Override
                public void onFinish(File file) {
                    appInstall(file, packageName);
                }

                @Override
                public void onError(Exception e) {
                    DAppApi.appInstalled(packageName, DOWNLOAD_FAILED, mDApp);
                }
            });
        } else {
            appInstall(apkFile, packageName);
        }
    }

    public void uninstallApp(String packageName) {
        Adb adb = new Adb(Touch.getInstance().getConnection());
        adb.uninstallApp(packageName, null);
    }

    public void startApp(String packageName) {
        mTaskHelper.launchApp(packageName);
    }

    private void appInstall(File file, final String packageName) {
        Adb adb = new Adb(Touch.getInstance().getConnection());
        adb.installApp(file.getAbsolutePath(), packageName, new ShellChannel.ShellListener() {
            @Override
            public void onStdout(ShellChannel ch, byte[] data) {
                DAppApi.appInstalled(packageName, SUCCESS, mDApp);
            }

            @Override
            public void onStderr(ShellChannel ch, byte[] data) {
                Log.e(TAG, "Install app failed. e = " + new String(data));

                DAppApi.appInstalled(packageName, INSTALL_FAILED, mDApp);
            }

            @Override
            public void onExit(ShellChannel ch, int code) {
            }
        });
    }
}
