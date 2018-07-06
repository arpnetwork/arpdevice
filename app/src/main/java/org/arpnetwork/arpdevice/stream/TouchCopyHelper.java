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

import android.os.AsyncTask;
import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TouchCopyHelper {
    private static final String TAG = "TouchCopyHelper";
    private static final String ARPTOUCH_FILE_NAME = "arptouch";
    private static final String ARP_PROPERTIES_NAME = "arp.properties";

    public static void copyTouchAsync(Callback callback) {
        CopyTouchTask task = new CopyTouchTask(callback);
        task.execute();
    }

    public interface Callback {
        void onResult(boolean success);
    }

    public static final class CopyTouchTask extends AsyncTask<Void, Void, Boolean> {
        private Callback func;

        public CopyTouchTask(Callback func) {
            this.func = func;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            return copyFromAsset();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            func.onResult(success);
        }
    }

    public static boolean copyFromAsset() {
        boolean success = true;

        String destFileName = "/data/local/tmp/" + ARPTOUCH_FILE_NAME;
        File destFile = new File(destFileName);
        try {
            if (!destFile.exists()) {
                Utils.copyFromAsset(ARPTOUCH_FILE_NAME, destFileName);
            } else if (!(getMd5FromAssets().equals(Utils.md5(destFile)))) {
                destFile.delete();
                Utils.copyFromAsset(ARPTOUCH_FILE_NAME, destFileName);
            }
        } catch (IOException e) {
            success = false;
        }

        return success;
    }

    private static String getMd5FromAssets() {
        String md5 = "";
        try {
            InputStream in = CustomApplication.sInstance.getAssets().open(ARP_PROPERTIES_NAME);
            md5 = getProperty(in, "md5");
        } catch (IOException ignored) {
        }
        return md5;
    }

    private static final String getProperty(InputStream in, String key) {
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