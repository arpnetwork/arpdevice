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

package org.arpnetwork.arpdevice.util;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.lang.reflect.Method;
import java.util.UUID;

public class DeviceUtil {
    private static final String KEY_MIUI_VERSION = "ro.miui.ui.version.name";
    private static final String KEY_EMUI_VERSION = "ro.build.version.emui";
    private static final String KEY_FUNTOUCH_VERSION = "ro.vivo.os.build.display.id";
    private static final String KEY_BORAD = "ro.board.platform";

    public static String getBrand() {
        return Build.BRAND;
    }

    public static String getModel() {
        return Build.MODEL;
    }

    public static String getIMSI(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String imsi = tm.getSubscriberId();
        return imsi != null ? imsi : "";
    }

    public static String getCpu() {
        String property = getSystemProperty(KEY_BORAD, "");
        String cpu = !TextUtils.isEmpty(property) ? property : Build.BOARD;
        return cpu.toLowerCase();
    }

    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    public static int getSdk() {
        return Build.VERSION.SDK_INT;
    }

    public static String getSysUIVersion() {
        String uiVersion = getAndroidVersion();
        String property = null;
        if ((property = getSystemProperty(KEY_MIUI_VERSION, null)) != null && !TextUtils.isEmpty(property)) {
            uiVersion = String.format("MIUI %s", property);
        } else if ((property = getSystemProperty(KEY_EMUI_VERSION, null)) != null && !TextUtils.isEmpty(property)) {
            uiVersion = property;
        } else if ((property = getSystemProperty(KEY_FUNTOUCH_VERSION, null)) != null && !TextUtils.isEmpty(property)) {
            uiVersion = property;
        } else if (Build.DISPLAY.toLowerCase().contains("flyme")) {
            uiVersion = Build.DISPLAY;
        }
        return uiVersion;
    }

    public static int[] getResolution(Context context) {
        WindowManager w = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display d = w.getDefaultDisplay();
        int widthPixels, heightPixels;
        try {
            Point realSize = new Point();
            Display.class.getMethod("getRealSize", Point.class).invoke(d, realSize);
            widthPixels = realSize.x;
            heightPixels = realSize.y;
        } catch (Exception e) {
            DisplayMetrics metrics = new DisplayMetrics();
            d.getMetrics(metrics);
            widthPixels = metrics.widthPixels;
            heightPixels = metrics.heightPixels;
        }
        return new int[]{widthPixels, heightPixels};
    }

    public static String getResolutionStr(Context context) {
        int[] r = getResolution(context);
        return String.format("%s*%s", r[0], r[1]);
    }

    public static String getUUID() {
        return UUID.randomUUID().toString();
    }

    public static long getMemoryTotal(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        manager.getMemoryInfo(memoryInfo);
        return memoryInfo.totalMem;
    }

    public static long getExternalDiskAvailable(Context context) {
        long availableBlocks = 0;
        if (Environment.MEDIA_MOUNTED.equalsIgnoreCase(Environment.getExternalStorageState())) {
            File file = Environment.getExternalStorageDirectory();
            StatFs statFs = new StatFs(file.getAbsolutePath());

            if (Build.VERSION.SDK_INT < 18) {
                int blockSize = statFs.getBlockSize();
                availableBlocks = blockSize * statFs.getAvailableBlocks();
            } else {
                long blockSize = statFs.getBlockSizeLong();
                availableBlocks = blockSize * statFs.getAvailableBlocksLong();
            }
        }
        return availableBlocks;
    }

    public static String getSystemProperty(String key, String defaultValue) {
        try {
            Class<?> clz = Class.forName("android.os.SystemProperties");
            Method get = clz.getMethod("get", String.class, String.class);
            return (String) get.invoke(clz, key, defaultValue);
        } catch (Exception e) {
        }
        return defaultValue;
    }
}
