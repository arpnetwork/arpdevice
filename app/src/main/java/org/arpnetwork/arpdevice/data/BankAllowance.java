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

import java.math.BigInteger;

public class BankAllowance {
    private static final String KEY = "bank_allowance";

    public BigInteger id;
    public BigInteger amount;
    public BigInteger paid;
    public BigInteger expired;
    public String proxy;

    public void save() {
        String json = new Gson().toJson(this);
        PreferenceManager.getInstance().putString(KEY, json);
    }

    public static BankAllowance get() {
        String json = PreferenceManager.getInstance().getString(KEY);
        if (!TextUtils.isEmpty(json)) {
            try {
                return new Gson().fromJson(json, BankAllowance.class);
            } catch (JsonSyntaxException e) {
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "BankBalance{" +
                "id=" + id +
                ", amount=" + amount +
                ", paid=" + paid +
                ", expired=" + expired +
                ", proxy='" + proxy + '\'' +
                '}';
    }
}
