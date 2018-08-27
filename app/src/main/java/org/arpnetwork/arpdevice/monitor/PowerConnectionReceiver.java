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

package org.arpnetwork.arpdevice.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.support.v4.content.LocalBroadcastManager;

import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.config.Constant;

public class PowerConnectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        sendBroadcast(isCharging);
    }

    private void sendBroadcast(boolean isCharging) {
        Intent localIntent = new Intent();
        localIntent.setAction(Constant.BROADCAST_ACTION_CHARGING);
        localIntent.putExtra(Constant.EXTENDED_DATA_CHARGING, isCharging);
        LocalBroadcastManager.getInstance(CustomApplication.sInstance).sendBroadcast(localIntent);
    }
}
