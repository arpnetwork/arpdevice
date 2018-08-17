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

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.arpnetwork.arpdevice.util.PreferenceManager;

public class Promise {
    private static final String KEY = "promise";

    private String cid;
    private String from;
    private String to;
    private String amount;
    private String sign;

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getCid() {
        return cid;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFrom() {
        return from;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getTo() {
        return to;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getAmount() {
        return amount;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getSign() {
        return sign;
    }

    public void save() {
        String json = new Gson().toJson(this);
        PreferenceManager.getInstance().putString(KEY, json);
    }

    public static Promise get() {
        String json = PreferenceManager.getInstance().getString(KEY);
        return fromJson(json);
    }

    public static Promise fromJson(String json) {
        if (!TextUtils.isEmpty(json)) {
            try {
                return new Gson().fromJson(json, Promise.class);
            } catch (JsonSyntaxException e) {
            }
        }
        return null;
    }
}
