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
import org.arpnetwork.arpdevice.constant.Constant;
import org.arpnetwork.arpdevice.util.DeviceUtil;
import org.arpnetwork.arpdevice.util.PreferenceManager;
import org.web3j.utils.Convert;

import java.math.BigInteger;

public class DeviceInfo {
    public String proxy;
    public String ver;
    public String brand;
    public String model;
    public String imsi;
    public String cpu;
    public String gpu;
    public long ram;
    public long storage;
    public int width;
    public int height;
    public BigInteger price;

    @SerializedName("tcp_port")
    public int tcpPort;

    @SerializedName("os_ver")
    public String osVer;

    @SerializedName("system_ver")
    public String systemVer;

    @SerializedName("connectivity")
    public int connectivity;

    @SerializedName("telephony")
    public int telephony;

    private static DeviceInfo sInstance;

    public static void init(Context context) {
        if (sInstance == null) {
            DeviceInfo info = new DeviceInfo();
            info.ver = Config.PROTOCOL_VERSION;
            info.brand = DeviceUtil.getBrand();
            info.model = DeviceUtil.getModel();
            info.imsi = DeviceUtil.getIMSI(context);
            info.cpu = DeviceUtil.getCpu();
            info.gpu = "";
            info.ram = DeviceUtil.getMemoryTotal(context);
            info.storage = DeviceUtil.getExternalDiskAvailable(context);
            info.width = DeviceUtil.getResolution(context)[0];
            info.height = DeviceUtil.getResolution(context)[1];
            info.osVer = DeviceUtil.getAndroidVersion();
            info.systemVer = DeviceUtil.getSysUIVersion();
            info.connectivity = -1;
            info.telephony = 0;
            info.price = Convert.toWei(String.valueOf(info.getPrice()), Convert.Unit.ETHER).toBigInteger();
            sInstance = info;
        }
    }

    public static DeviceInfo get() {
        return sInstance;
    }

    public void setPrice(int price) {
        this.price = Convert.toWei(String.valueOf(price), Convert.Unit.ETHER).toBigInteger();
        PreferenceManager.getInstance().putInt(Constant.ORDER_PRICE, price);
    }

    public int getPrice() {
        int orderPrice = PreferenceManager.getInstance().getInt(Constant.ORDER_PRICE);
        return orderPrice >= 0 ? orderPrice : Config.ORDER_PRICE_DEFAULT;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public boolean isProxyMode() {
        return this.proxy != null;
    }

    public void setDataPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }
}
