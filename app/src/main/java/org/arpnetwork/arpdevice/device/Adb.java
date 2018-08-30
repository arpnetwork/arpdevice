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

import org.arpnetwork.adb.Connection;
import org.arpnetwork.adb.ShellChannel;
import org.arpnetwork.adb.SyncChannel;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.stream.AssetCopyHelper;
import org.arpnetwork.arpdevice.stream.Touch;

import java.io.File;

public class Adb {
    private static final String TAG = "Adb";
    private static final boolean DEBUG = Config.DEBUG;

    private final Connection mConnection;

    public Adb(Connection syncChannel) {
        this.mConnection = syncChannel;
    }

    public void getTopAndroidTask(ShellChannel.ShellListener listener) {
        if (Touch.getInstance().getState() == Touch.STATE_CONNECTED) {
            ShellChannel ss = mConnection.openShell("dumpsys activity top | grep TASK");
            ss.setListener(listener);
        }
    }

    public void installApp(String srcFilePath, String packageName, final ShellChannel.ShellListener listener) {
        if (Touch.getInstance().getState() == Touch.STATE_CONNECTED) {
            final String desFile = "/data/local/tmp/" + packageName;
            if (new File(desFile).exists()) {
                new File(desFile).delete();
            }

            try {
                SyncChannel ss = mConnection.openSync();
                AssetCopyHelper.pushFile(ss, desFile, srcFilePath, new AssetCopyHelper.PushCallback() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onComplete(boolean success, Throwable throwable) {
                        if (success) {
                            ShellChannel ss = mConnection.openShell("pm install  -r " + desFile);
                            ss.setListener(listener);
                        } else {
                            if (listener != null) {
                                listener.onStderr(null, null);
                            }
                        }
                    }
                });
            } catch (Exception e) {
                if (listener != null) {
                    listener.onStderr(null, null);
                }
            }
        } else {
            if (listener != null) {
                listener.onStderr(null, null);
            }
        }
    }

    public void uninstallApp(String packageName, final ShellChannel.ShellListener listener) {
        if (Touch.getInstance().getState() == Touch.STATE_CONNECTED) {
            ShellChannel ss = mConnection.openShell("pm uninstall " + packageName);
            ss.setListener(listener);
        } else {
            if (listener != null) {
                listener.onStderr(null, null);
            }
        }
    }

    public void killApp(String packageName) {
        if (Touch.getInstance().getState() == Touch.STATE_CONNECTED) {
            mConnection.openShell(String.format("am force-stop %s", packageName));
        }
    }

    public void clearApplicationUserData(String packageName) {
        if (Touch.getInstance().getState() == Touch.STATE_CONNECTED) {
            mConnection.openShell("pm clear " + packageName);
        }
    }
}
