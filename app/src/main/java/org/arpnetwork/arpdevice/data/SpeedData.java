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

import com.google.gson.annotations.SerializedName;

import org.arpnetwork.arpdevice.util.Util;

public class SpeedData {
    private String hash;

    @SerializedName("upload_speed")
    private long uploadSpeed;

    @SerializedName("download_speed")
    private long downloadSpeed;

    public SpeedData(byte[] data) {
        this.hash = Util.md5(data);
    }

    public String getHash() {
        return hash;
    }

    public long getDownloadSpeed() {
        return downloadSpeed;
    }

    public long getUploadSpeed() {
        return uploadSpeed;
    }
}
