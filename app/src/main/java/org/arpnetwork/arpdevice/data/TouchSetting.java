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

package org.arpnetwork.arpdevice.data;

import android.text.TextUtils;

import org.arpnetwork.arpdevice.Touch;

public class TouchSetting {
    public int contacts;
    public int x;
    public int y;
    public int pressure;
    public int major;
    public int minor;

    public TouchSetting(int contacts, int x, int y, int pressure, int major, int minor) {
        this.contacts = contacts;
        this.x = x;
        this.y = y;
        this.pressure = pressure;
        this.major = major;
        this.minor = minor;
    }

    public static TouchSetting createTouchSetting() {
        String banner = Touch.getInstance().getBanner();  // 10 1440 2560 0 255 255
        if (!TextUtils.isEmpty(banner)) {
            try {
                int contacts = Integer.parseInt(banner.split(" ")[0]);
                int x = Integer.parseInt(banner.split(" ")[1]);
                int y = Integer.parseInt(banner.split(" ")[2]);
                int pressure = Integer.parseInt(banner.split(" ")[3]);
                int major = Integer.parseInt(banner.split(" ")[4]);
                int minor = Integer.parseInt(banner.split(" ")[5]);
                return new TouchSetting(contacts, x, y, pressure, major, minor);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
