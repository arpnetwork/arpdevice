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

package org.arpnetwork.arpdevice.app;

import android.text.TextUtils;

import org.arpnetwork.arpdevice.util.PreferenceManager;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

public class AtomicNonce {

    public static void sync(String nonce, String address) {
        increment(Numeric.cleanHexPrefix(nonce), address);
    }

    public static String getAndIncrement(String address) {
        String nonce = PreferenceManager.getInstance().getString(address);
        if (TextUtils.isEmpty(nonce)) {
            nonce = "1";
        }

        increment(nonce, address);
        return Numeric.prependHexPrefix(nonce);
    }

    private static void increment(String nonce, String address) {
        BigInteger bigNonce = new BigInteger(nonce, 16);
        bigNonce = bigNonce.add(new BigInteger("1", 16));
        PreferenceManager.getInstance().putString(address, bigNonce.toString(16));
    }
}
