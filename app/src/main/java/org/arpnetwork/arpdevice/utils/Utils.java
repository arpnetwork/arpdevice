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

package org.arpnetwork.arpdevice.utils;

import android.content.res.AssetManager;

import org.arpnetwork.adb.SyncChannel;
import org.arpnetwork.arpdevice.CustomApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    private Utils() {
        // prevent initial.
    }

    public static void copy(InputStream in, OutputStream out) {
        if (in != null && out != null) {
            int BUFFER_SIZE = 1024 * 8;
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            try {
                while ((len = in.read(buffer, 0, buffer.length)) != -1) {
                    out.write(buffer, 0, len);
                    out.flush();
                }
            } catch (IOException ignored) {
            } finally {
                try {
                    out.close();
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static void copy(InputStream in, SyncChannel out) throws IOException {
        if (in != null && out != null) {
            int BUFFER_SIZE = 1024 * 8;
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            try {
                while ((len = in.read(buffer, 0, buffer.length)) != -1) {
                    out.writeData(buffer, 0, len);
                }
                out.writeDone((int) (System.currentTimeMillis() / 1000));
            } finally {
                try {
                    out.close();
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static String md5(File file) {
        String md5 = null;
        MessageDigest digest = null;
        if (file != null && file.isFile()) {
            FileInputStream in = null;
            byte buffer[] = new byte[1024];
            int len = 0;
            try {
                digest = MessageDigest.getInstance("MD5");
                in = new FileInputStream(file);

                while ((len = in.read(buffer, 0, buffer.length)) != -1) {
                    digest.update(buffer, 0, len);
                }

                BigInteger bigInt = new BigInteger(1, digest.digest());
                md5 = bigInt.toString(16);
            } catch (NoSuchAlgorithmException ignored) {
            } catch (FileNotFoundException ignored) {
            } catch (IOException ignored) {
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ignored) {
                }
            }

        }

        return md5;
    }

    public static void copyFromAsset(String assetFileName, SyncChannel syncChannel) throws IOException {
        AssetManager assetManager = CustomApplication.sInstance.getAssets();
        Utils.copy(assetManager.open(assetFileName), syncChannel);
    }
}
