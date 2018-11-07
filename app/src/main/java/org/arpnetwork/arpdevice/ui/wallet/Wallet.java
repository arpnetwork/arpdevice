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
import org.arpnetwork.arpdevice.util.Util;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.io.File;

/**
 * Wallet keystore is imported by user and used for token keeping and transaction signing.
 * Account keystore is created automatically, used for signing protocol data, but keep no token.
 */

public class Wallet {
    private static final String KEYSTORE_PATH = "keystore_path";
    private static final String WALLET_ADDRESS = "wallet_address";

    private static final String ACCOUNT_PATH = "account_path";
    private static final String ACCOUNT_PASSWORD = "account_password";
    private static final String ACCOUNT_ADDRESS = "account_address";

    private String address;

    public interface Callback {
        void onCompleted(boolean success);
    }

    public Wallet() {
    }

    public Wallet(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void save() {
        putStorageString(WALLET_ADDRESS, address);
    }

    public static Wallet get() {
        Wallet wallet = new Wallet(getStorageString(WALLET_ADDRESS));
        return wallet;
    }

    public static String getAccountAddress() {
        String accountAddress = getStorageString(ACCOUNT_ADDRESS);
        return accountAddress.length() > 0 ? accountAddress : null;
    }

    public static boolean exists() {
        String path = getStorageString(KEYSTORE_PATH);
        if (!TextUtils.isEmpty(path) && new File(path).exists() && getAccountAddress() != null) {
            return true;
        }
        return false;
    }

    public static void importWallet(final Context context, final String privateKey, final String password, final Callback callback) {
        final File destDir = getDestDir(context);
        if (destDir == null) {
            if (callback != null) {
                callback.onCompleted(false);
            }
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Credentials credentials = Credentials.create(privateKey);
                    Wallet wallet = new Wallet(credentials.getAddress());
                    wallet.save();

                    String fileName = WalletUtils.generateWalletFile(password, credentials.getEcKeyPair(), destDir, false);
                    putStorageString(KEYSTORE_PATH, new File(destDir, fileName).getAbsolutePath());

                    createAccount(context);

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

    public static Credentials loadWalletCredentials(String password) {
        return loadCredentials(password, KEYSTORE_PATH);
    }

    public static Credentials loadAccountCredentials() {
        return loadCredentials(getStorageString(ACCOUNT_PASSWORD), ACCOUNT_PATH);
    }

    private static Credentials loadCredentials(String password, String type) {
        String path = getStorageString(type);
        try {
            Credentials credentials = WalletUtils.loadCredentials(password, new File(path));
            return credentials;
        } catch (Exception e) {
        }
        return null;
    }

    private static void createAccount(Context context) throws Exception {
        String path = getStorageString(ACCOUNT_PATH);
        if (path == null || path.length() == 0) {
            String password = Util.getRandomString(9);
            putStorageString(ACCOUNT_PASSWORD, password);
            File file = getDestDir(context);
            String keystoreFileName = WalletUtils.generateNewWalletFile(password, file, false);

            File keystoreFile = new File(file.getPath(), keystoreFileName);
            putStorageString(ACCOUNT_PATH, keystoreFile.getAbsolutePath());
            putStorageString(ACCOUNT_ADDRESS, WalletUtils.loadCredentials(password, keystoreFile).getAddress());
        }
    }

    private static File getDestDir(Context context) {
        final File destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (!destDir.exists()) {
            if (!destDir.mkdirs()) {
                return null;
            }
        }
        return destDir;
    }

    private static String getStorageString(String key) {
        return PreferenceManager.getInstance().getString(key);
    }

    private static void putStorageString(String key, String value) {
        PreferenceManager.getInstance().putString(key, value);
    }
}
