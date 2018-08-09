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

package org.arpnetwork.arpdevice.ui.wallet;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import org.arpnetwork.arpdevice.util.PreferenceManager;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.io.File;

public class Wallet {
    public static final String KEYSTORE_PATH = "keystore_path";

    private static final String NAME = "name";
    private static final String PUBLIC_KEY = "public_key";

    private String name;
    private String publicKey;

    public interface Callback {
        void onCompleted(boolean success);
    }

    public Wallet() {
    }

    public Wallet(String name, String publicKey) {
        this.name = name;
        this.publicKey = publicKey;
    }

    public String getName() {
        return name;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void save() {
        PreferenceManager.getInstance().putString(NAME, name);
        PreferenceManager.getInstance().putString(PUBLIC_KEY, publicKey);
    }

    public static Wallet get() {
        Wallet wallet = new Wallet();
        wallet.name = PreferenceManager.getInstance().getString(NAME);
        wallet.publicKey = PreferenceManager.getInstance().getString(PUBLIC_KEY);
        return wallet;
    }

    public static boolean exists() {
        String path = PreferenceManager.getInstance().getString(KEYSTORE_PATH);
        if (!TextUtils.isEmpty(path) && new File(path).exists()) {
            return true;
        }
        return false;
    }

    public static void importWallet(Context context, final String walletName, final String privateKey, final String password, final Callback callback) {
        final File destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (!destDir.exists()) {
            if (!destDir.mkdirs()) {
                if (callback != null) {
                    callback.onCompleted(false);
                }
                return;
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Credentials credentials = Credentials.create(privateKey);
                    Wallet wallet = new Wallet(walletName, credentials.getAddress());
                    wallet.save();

                    String fileName = WalletUtils.generateWalletFile(password, credentials.getEcKeyPair(), destDir, false);
                    PreferenceManager.getInstance().putString(KEYSTORE_PATH, new File(destDir, fileName).getAbsolutePath());

                    if (callback != null) {
                        callback.onCompleted(true);
                    }
                } catch (Exception e) {
                    if (callback != null) {
                        callback.onCompleted(false);
                    }
                }
            }
        }).start();
    }

    public static Credentials loadCredentials(String password) {
        String path = PreferenceManager.getInstance().getString(KEYSTORE_PATH);
        try {
            Credentials credentials = WalletUtils.loadCredentials(password, new File(path));
            return credentials;
        } catch (Exception e) {
        }
        return null;
    }
}
