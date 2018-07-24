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

import android.content.Context;
import android.text.TextUtils;

import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.util.UIHelper;

public class TouchSetting {
    public int contacts;
    public int x;
    public int y;
    public int pressure;
    public int major;
    public int minor;
    public int statusBarHeight;

    public TouchSetting(int contacts, int x, int y, int pressure, int major, int minor, int statusBarHeight) {
        this.contacts = contacts;
        this.x = x;
        this.y = y;
        this.pressure = pressure;
        this.major = major;
        this.minor = minor;
        this.statusBarHeight = statusBarHeight;
    }

    public static TouchSetting createTouchSetting() {
        String banner = Touch.getInstance().getBanner();
        if (!TextUtils.isEmpty(banner)) {
            try {
                Context context = CustomApplication.sInstance;
                String[] items = banner.split(" ");
                int contacts = Integer.parseInt(items[0]);
                int x = Integer.parseInt(items[1]);
                int getDisplayHeight = UIHelper.getDisplayHeight(context);
                int pressure = Integer.parseInt(items[3]);
                int major = Integer.parseInt(items[4]);
                int minor = Integer.parseInt(items[5]);
                return new TouchSetting(contacts, x, getDisplayHeight, pressure, major, minor,
                        UIHelper.getStatusbarHeight(context));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
