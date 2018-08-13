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
import org.arpnetwork.arpdevice.device.Adb;
import org.arpnetwork.arpdevice.device.TaskHelper;
import org.arpnetwork.arpdevice.download.DownloadManager;
import org.arpnetwork.arpdevice.download.IDownloadListener;
import org.arpnetwork.arpdevice.stream.Touch;

import java.io.File;

public class AppManager {
    private static final String TAG = AppManager.class.getSimpleName();
    private static TaskHelper sTaskHelper;

    static {
        sTaskHelper = new TaskHelper();
    }

    public static void appInstall(Context context, final String packageName, String url, int filesize, String md5) {
        File destDir = context.getFilesDir();
        if (!destDir.exists()) {
            if (!destDir.mkdirs()) {
                return;
            }
        }

        DownloadManager.getInstance().start(url, new File(destDir, String.format("%s.apk", packageName)), new IDownloadListener() {
            @Override
            public void onFinish(File file) {
                Adb adb = new Adb(Touch.getInstance().getConnection());
                adb.installApp(file.getAbsolutePath(), packageName, new ShellChannel.ShellListener() {
                    @Override
                    public void onStdout(ShellChannel ch, byte[] data) {
                    }

                    @Override
                    public void onStderr(ShellChannel ch, byte[] data) {
                        Log.e(TAG, "Install app failed. e = " + new String(data));
                    }

                    @Override
                    public void onExit(ShellChannel ch, int code) {
                    }
                });
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    public static void uninstallApp(String packageName) {
        Adb adb = new Adb(Touch.getInstance().getConnection());
        adb.uninstallApp(packageName, new ShellChannel.ShellListener() {
            @Override
            public void onStdout(ShellChannel ch, byte[] data) {
            }

            @Override
            public void onStderr(ShellChannel ch, byte[] data) {
                Log.e(TAG, "Uninstall app failed. e = " + new String(data));
            }

            @Override
            public void onExit(ShellChannel ch, int code) {
            }
        });
    }

    public static void startApp(String packageName) {
        sTaskHelper.launchApp(packageName);
    }
}
