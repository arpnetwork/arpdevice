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

package org.arpnetwork.arpdevice.util;

import org.arpnetwork.arpdevice.contracts.api.VerifyAPI;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A tool for signing the protocol data, but not for the transaction data.
 */
public class SignUtil {

    /**
     * Check whether the signer exists
     */
    public static boolean signerExists() {
        return Wallet.getAccountAddress() != null;
    }

    /**
     * Sign the protocol data with account credentials
     */
    public static String signWithAccount(String data) {
        return !isTransactionData(data) ? VerifyAPI.sign(data, Wallet.loadAccountCredentials()) : null;
    }

    /**
     * Sign the protocol data with wallet credentials
     */
    public static String signWithWallet(String password, String data) {
        return !isTransactionData(data) ? VerifyAPI.sign(data, Wallet.loadWalletCredentials(password)) : null;
    }

    private static boolean isTransactionData(String data) {
        try {
            JSONObject object = new JSONObject(data);
            return object.has("gasPrice") && object.has("gasLimit") && object.has("to") && object.has("value");
        } catch (JSONException e) {
        }
        return false;
    }
}
