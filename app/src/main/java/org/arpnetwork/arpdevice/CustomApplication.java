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
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.ui.miner.MonitorService;
import org.arpnetwork.arpdevice.util.PreferenceManager;
import org.arpnetwork.arpdevice.util.NetworkHelper;

public class CustomApplication extends Application {
    public static CustomApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        PreferenceManager.init(this);
        NetworkHelper.init(getApplicationContext());

        DeviceInfo.init(this);
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

        Touch.getInstance().close();
        PreferenceManager.fini();
        NetworkHelper.fini();

        System.exit(0);
    }

    public void startMonitorService() {
        Intent startServiceIntent = new Intent(this, MonitorService.class);
        startService(startServiceIntent);
    }

    public void stopMonitorService() {
        Intent startServiceIntent = new Intent(this, MonitorService.class);
        stopService(startServiceIntent);
    }
}
