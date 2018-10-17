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
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.util.DeviceUtil;

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

    public void getInstalledApps(ShellChannel.ShellListener listener) {
        if (Touch.getInstance().getState() == Touch.STATE_CONNECTED) {
            ShellChannel ss = mConnection.openShell("pm list package");
            ss.setListener(listener);
        }
    }

    public void installApp(String srcFilePath, ShellChannel.ShellListener listener) {
        if (Touch.getInstance().getState() == Touch.STATE_CONNECTED) {
            if (DeviceUtil.getSdk() >= 28) { // fix android P install.
                ShellChannel ss = mConnection.openShell("cat " + srcFilePath + " | pm install -S " + new File(srcFilePath).length());
                ss.setListener(listener);
            } else {
                ShellChannel ss = mConnection.openShell("pm install -r " + srcFilePath);
                ss.setListener(listener);
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

    public void getUIInfo(String path, ShellChannel.ShellListener listener) {
        if (Touch.getInstance().getState() == Touch.STATE_CONNECTED) {
            ShellChannel ss = mConnection.openShell(String.format("uiautomator dump %s", path));
            ss.setListener(listener);
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

    public void stayOn() {
        if (Touch.getInstance().getState() == Touch.STATE_CONNECTED) {
            mConnection.openShell("settings put global stay_on_while_plugged_in 7");
        }
    }

    public void globalDimOn(final int screenBrightness, final ShellChannel.ShellListener listener) {
        if (screenBrightness < 0 || screenBrightness > 255) return;

        if (Touch.getInstance().getState() == Touch.STATE_CONNECTED) {
            ShellChannel shellChannel = mConnection.openShell("settings get system screen_brightness_mode && settings get system screen_brightness");
            shellChannel.setListener(new ShellChannel.ShellListener() {
                @Override
                public void onStdout(ShellChannel ch, byte[] data) {
                    mConnection.openShell("settings put system screen_brightness_mode 0 && settings put system screen_brightness " + screenBrightness);

                    if (listener != null) {
                        listener.onStdout(ch, data);
                    }
                }

                @Override
                public void onStderr(ShellChannel ch, byte[] data) {
                    if (listener != null) {
                        listener.onStderr(ch, data);
                    }
                }

                @Override
                public void onExit(ShellChannel ch, int code) {
                    if (listener != null) {
                        listener.onExit(ch, code);
                    }
                }
            });
        }
    }

    public void globalDimRestore(int screenBrightnessMode, int screenBrightness) {
        if (screenBrightnessMode < 0 || screenBrightnessMode > 1) return;
        if (screenBrightness < 0 || screenBrightness > 255) return;

        if (Touch.getInstance().getState() == Touch.STATE_CONNECTED) {
            mConnection.openShell("settings put system screen_brightness_mode " + screenBrightnessMode + " && " + "settings put system screen_brightness " + screenBrightness);
        }
    }
}
