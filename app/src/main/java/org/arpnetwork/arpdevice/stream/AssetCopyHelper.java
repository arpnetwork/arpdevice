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

import org.arpnetwork.adb.Channel;
import org.arpnetwork.adb.SyncChannel;
import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.util.DeviceUtil;
import org.arpnetwork.arpdevice.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AssetCopyHelper {
    private static final String TAG = "AssetCopyHelper";
    private static final boolean DEBUG = Config.DEBUG;

    private static final String DIR = "/data/local/tmp/";
    private static final String ARPTOUCH_FILE_NAME = "arptouch";
    private static final String ARP_PROPERTIES_NAME = "arp.properties";
    private static final String DEST_CAP_FILE_NAME = "arpcap";
    private static final String DEST_LIB_ARPCAP_FILE_NAME = "libarpcap.so";

    private static final String ABI_TARGET;
    private static final String ASSET_ARPCAP_FILE_NAME;
    private static final String ASSET_LIB_ARPCAP_FILE_NAME;

    static {
        if (DeviceUtil.is64bit()) {
            ABI_TARGET = "arm64-v8a";
        } else {
            ABI_TARGET = "armeabi-v7a";
        }
        ASSET_ARPCAP_FILE_NAME = ABI_TARGET + "/arpcap";
        ASSET_LIB_ARPCAP_FILE_NAME = ABI_TARGET + "/libarpcap.so";
    }

    public interface PushCallback {
        void onStart();

        void onComplete(boolean success, Throwable throwable);
    }

    public static void pushTouch(final SyncChannel ss, PushCallback listener) {
        pushFileFromAsset(ss, DIR + ARPTOUCH_FILE_NAME, ARPTOUCH_FILE_NAME, listener);
    }

    public static void pushCap(final SyncChannel ss, PushCallback listener) {
        pushFileFromAsset(ss, DIR + DEST_CAP_FILE_NAME, ASSET_ARPCAP_FILE_NAME, listener);
    }

    public static void pushLibCap(final SyncChannel ss, PushCallback listener) {
        pushFileFromAsset(ss, DIR + DEST_LIB_ARPCAP_FILE_NAME, ASSET_LIB_ARPCAP_FILE_NAME, listener);
    }

    public static void pushFile(final SyncChannel ss, final String destFilePath,
            final String srcFilePath, final PushCallback listener) {

        ss.setStreamListener(new Channel.ChannelListener() {
            @Override
            public void onOpened(Channel ch) {
                if (listener != null) {
                    listener.onStart();
                }

                ss.send(destFilePath, SyncChannel.MODE_EXECUTABLE);

                try {
                    Util.copy(new FileInputStream(new File(srcFilePath)), ss);
                } catch (IOException e) {
                    if (listener != null) {
                        listener.onComplete(false, e);
                    }
                }
            }

            @Override
            public void onClosed(Channel ch) {
                if (listener != null) {
                    listener.onComplete(true, null);
                }
            }
        });
    }

    public static boolean isValidTouchBinary() {
        return isValidFile(DIR + ARPTOUCH_FILE_NAME, "touch_md5");
    }

    public static boolean isValidCapBinary() {
        return isValidFile(DIR + DEST_CAP_FILE_NAME, ABI_TARGET + "-" + "cap_md5");
    }

    public static boolean isValidCapLib() {
        return isValidFile(DIR + DEST_LIB_ARPCAP_FILE_NAME, ABI_TARGET + "-" + "lib_cap_md5");
    }

    public static boolean isValidFile(String destFilePath, String keyOfMd5) {
        boolean success = true;

        File destFile = new File(destFilePath);
        if (!destFile.exists()) {
            success = false;
        } else if (!(getMd5FromAssets(keyOfMd5).equals(Util.md5(destFile)))) {
            success = false;
        }

        return success;
    }

    public static void pushFileFromAsset(final SyncChannel ss, final String destFilePath,
            final String assetFileName, final PushCallback listener) {

        ss.setStreamListener(new Channel.ChannelListener() {
            @Override
            public void onOpened(Channel ch) {
                if (listener != null) {
                    listener.onStart();
                }

                ss.send(destFilePath, SyncChannel.MODE_EXECUTABLE);

                try {
                    Util.copy(CustomApplication.sInstance.getAssets().open(assetFileName), ss);
                } catch (IOException e) {
                    if (listener != null) {
                        listener.onComplete(false, e);
                    }
                }
            }

            @Override
            public void onClosed(Channel ch) {
                if (listener != null) {
                    listener.onComplete(true, null);
                }
            }
        });
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