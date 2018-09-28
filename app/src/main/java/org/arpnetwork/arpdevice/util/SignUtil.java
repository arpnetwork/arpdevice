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
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.io.File;

/**
 * A tool for signing the protocol data, but not for the transaction data.
 * First you should create a signer through {@link SignUtil#generateSigner}.
 * Before the method {@link SignUtil#sign} is invoked, confirm whether the signer exists
 * through {@link SignUtil#signerExists}.
 */
public class SignUtil {
    private static Signer sSigner;

    /**
     * Check whether the signer exists
     */
    public static boolean signerExists() {
        return sSigner != null;
    }

    /**
     * Sign the protocol data
     */
    public static String sign(String data) {
        return sSigner != null ? sSigner.sign(data) : null;
    }

    /**
     * Create a signer
     */
    public static void generateSigner(String password) {
        String path = PreferenceManager.getInstance().getString(Wallet.KEYSTORE_PATH);
        try {
            final Credentials credentials = WalletUtils.loadCredentials(password, new File(path));
            sSigner = new Signer() {
                @Override
                public String sign(String data) {
                    return !isTransactionData(data) ? VerifyAPI.sign(data, credentials) : null;
                }
            };
        } catch (Exception e) {
        }
    }

    /**
     * Reset signer
     */
    public static void resetSigner() {
        sSigner = null;
    }

    private static boolean isTransactionData(String data) {
        try {
            JSONObject object = new JSONObject(data);
            return object.has("gasPrice") && object.has("gasLimit") && object.has("to") && object.has("value");
        } catch (JSONException e) {
        }
        return false;
    }

    interface Signer {
        String sign(String data);
    }
}
