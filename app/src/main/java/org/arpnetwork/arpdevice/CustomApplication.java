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

package org.arpnetwork.arpdevice;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import org.arpnetwork.arpdevice.data.DeviceInfo;
import org.arpnetwork.arpdevice.server.DataServer;
import org.arpnetwork.arpdevice.stream.RecordService;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.util.PreferenceManager;
import org.arpnetwork.arpdevice.util.NetworkHelper;

public class CustomApplication extends Application {
    public static CustomApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        DeviceInfo.create(this);
        PreferenceManager.init(this);
        NetworkHelper.init(getApplicationContext());
        Touch.getInstance().connect();
        DataServer.getInstance().startServer();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        Intent recordIntent = new Intent(this, RecordService.class);
        stopService(recordIntent);

        DataServer.getInstance().shutdown();
        PreferenceManager.fini();
        NetworkHelper.fini();

        System.exit(0);
    }
}
