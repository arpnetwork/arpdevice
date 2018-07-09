/*
 *
 * Copyright 2018 ARP Network
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.arpnetwork.arpdevice.stream;

import android.os.Environment;

import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AssetCopyHelper {
    private static final String TAG = "AssetCopyHelper";
    private static final String ARPTOUCH_FILE_NAME = "arptouch";
    private static final String ARP_PROPERTIES_NAME = "arp.properties";

    public static boolean copyTouch2Sdcard() {
        return copyAsset2Sdcard(ARPTOUCH_FILE_NAME, "md5");
    }

    public static boolean copyAsset2Sdcard(String assetFileName, String keyOfMd5) {
        boolean success = true;

        File sdcardFile = new File(Environment.getExternalStorageDirectory() + File.separator + assetFileName);
        try {
            if (!sdcardFile.exists()) {
                Utils.copyFromAsset(assetFileName, sdcardFile);
            } else if (!(getMd5FromAssets(keyOfMd5).equals(Utils.md5(sdcardFile)))) {
                sdcardFile.delete();
                Utils.copyFromAsset(assetFileName, sdcardFile);
            }
        } catch (IOException e) {
            success = false;
        }

        return success;
    }

    public static boolean isValidTouchBinary() {
        return isValidBinary(ARPTOUCH_FILE_NAME, "md5");
    }

    public static boolean isValidBinary(String destFileName, String keyOfMd5) {
        boolean success = true;

        File destFile = new File("/data/local/tmp/" + destFileName);
        if (!destFile.exists()) {
            success = false;
        } else if (!(getMd5FromAssets(keyOfMd5).equals(Utils.md5(destFile)))) {
            success = false;
        }

        return success;
    }

    private static String getMd5FromAssets(String key) {
        String md5 = "";
        try {
            InputStream in = CustomApplication.sInstance.getAssets().open(ARP_PROPERTIES_NAME);
            md5 = getProperty(in, key);
        } catch (IOException ignored) {
        }
        return md5;
    }

    private static String getProperty(InputStream in, String key) {
        String value = null;

        if (in != null) {
            Properties properties = new Properties();
            try {
                properties.load(in);
                value = properties.getProperty(key);
            } catch (IOException ignored) {
            } finally {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }

        return value;
    }
}