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

    public void killApp(String packageName) {
        if (Touch.getInstance().getState() == Touch.STATE_CONNECTED) {
            mConnection.openShell(String.format("am force-stop %s", packageName));
        }
    }
}
