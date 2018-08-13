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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DownloadManager {
    private static final String TAG = DownloadManager.class.getSimpleName();

    private static DownloadManager sManager;

    private static final List<DownloadTask> TASKS = Collections.synchronizedList(new ArrayList<DownloadTask>());

    public static DownloadManager getInstance() {
        if (sManager == null) {
            sManager = new DownloadManager();
        }
        return sManager;
    }

    public void start(String url, File file, IDownloadListener listener) {
        DownloadTask task = new DownloadTask(url, file, listener);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        TASKS.add(task);
    }

    public void cancelAll() {
        for (DownloadTask task : TASKS) {
            task.cancel(true);
        }
        TASKS.clear();
    }

    public void removeTask(DownloadTask task) {
        TASKS.remove(task);
    }

    private DownloadManager() {
    }
}
