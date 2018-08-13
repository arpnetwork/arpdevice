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

package org.arpnetwork.arpdevice.download;

import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadTask extends AsyncTask<Void, Void, Boolean> {
    private static final String TAG = DownloadTask.class.getSimpleName();
    private static final int MAX_REDIRECTS = 2;
    private static final int DEFAULT_TIMEOUT = 20000;

    private String mUrl;
    private File mFile;
    private IDownloadListener mListener;
    private int mRedirect;

    public DownloadTask(String url, File file, IDownloadListener listener) {
        mUrl = url;
        mFile = file;
        mListener = listener;
        mRedirect = 0;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return openConnection(mUrl, mFile);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (!isCancelled()) {
            if (mListener != null) {
                if (success) {
                    mListener.onFinish(mFile);
                } else {
                    mListener.onError(new Exception("download error"));
                }
            }
            DownloadManager.getInstance().removeTask(this);
        }
    }

    private boolean openConnection(String url, File file) {
        while (mRedirect < MAX_REDIRECTS) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(DEFAULT_TIMEOUT);
                conn.setReadTimeout(DEFAULT_TIMEOUT);
                conn.setInstanceFollowRedirects(false);

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    FileOutputStream fos = new FileOutputStream(file);
                    byte[] buf = new byte[64 * 1024];
                    int len;
                    while (!isCancelled() && (len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    fos.close();
                    is.close();

                    return true;
                } else if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP) {
                    url = conn.getHeaderField("location");
                    mRedirect++;
                }
            } catch (Exception e) {
                return false;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        return false;
    }
}

