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

package org.arpnetwork.arpdevice.data;

import android.content.Context;

import com.google.gson.annotations.SerializedName;

import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.server.DataServer;
import org.arpnetwork.arpdevice.util.DeviceUtil;

public class DeviceInfo {
    public String ver;
    public String id;
    public int port;
    public String brand;
    public String model;
    public String imsi;
    public String cpu;
    public String gpu;
    public long ram;
    public long storage;
    public String resolution;

    @SerializedName("os_ver")
    public String osVer;

    @SerializedName("system_ver")
    public String systemVer;

    @SerializedName("conn_net_type")
    public int connNetType;

    @SerializedName("tel_net_type")
    public int telNetType;

    private static DeviceInfo sInstance;

    public static void create(Context context) {
        DeviceInfo info = new DeviceInfo();
        info.ver = Config.PROTOCOL_VERSION;
        info.id = DeviceUtil.getUUID();
        info.port = DataServer.PORT;
        info.brand = DeviceUtil.getBrand();
        info.model = DeviceUtil.getModel();
        info.imsi = DeviceUtil.getIMSI(context);
        info.cpu = DeviceUtil.getCpu();
        info.gpu = "";
        info.ram = DeviceUtil.getMemoryTotal(context);
        info.storage = DeviceUtil.getExternalDiskAvailable(context);
        info.resolution = DeviceUtil.getResolutionStr(context);
        info.osVer = DeviceUtil.getAndroidVersion();
        info.systemVer = DeviceUtil.getSysUIVersion();
        info.connNetType = -1;
        info.telNetType = 0;
        sInstance = info;
    }

    public static DeviceInfo get() {
        return sInstance;
    }
}
