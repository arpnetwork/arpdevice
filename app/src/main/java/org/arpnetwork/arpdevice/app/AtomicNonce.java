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

import java.math.BigInteger;

public class AtomicNonce {
    private static final String NONCE = "nonce";

    public static String getAndIncrement() {
        String nonce = PreferenceManager.getInstance().getString(NONCE);
        if (TextUtils.isEmpty(nonce)) {
            nonce = "1";
        }

        BigInteger bigNonce = new BigInteger(nonce, 16);
        bigNonce = bigNonce.add(new BigInteger("1", 16));
        PreferenceManager.getInstance().putString(NONCE, bigNonce.toString(16));
        return String.format("0x%s", nonce);
    }
}
