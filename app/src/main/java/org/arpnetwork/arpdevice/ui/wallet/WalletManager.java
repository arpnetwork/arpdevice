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
import android.util.Log;

import org.arpnetwork.arpdevice.util.PreferenceManager;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.io.File;

public class WalletManager {
    private static final String TAG = WalletManager.class.getSimpleName();
    private static final String PRIVATE_KEY_PATH = "private_key_path";

    private static WalletManager sInstance;

    private Wallet mWallet;
    private Credentials mCredentials;

    public interface Callback {
        void onCompleted(boolean success);
    }

    public static WalletManager getInstance() {
        if (sInstance == null) {
            sInstance = new WalletManager();
        }
        return sInstance;
    }

    public void importWallet(Context context, Wallet wallet, final String password, final Callback callback) {
        final File destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (!destDir.exists()) {
            if (!destDir.mkdirs()) {
                if (callback != null) {
                    callback.onCompleted(false);
                }
                return;
            }
        }

        mWallet = wallet;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mCredentials = Credentials.create(mWallet.getPrivateKey());
                    mWallet.setPublicKey(mCredentials.getAddress());
                    mWallet.save();

                    String fileName = WalletUtils.generateWalletFile(password, mCredentials.getEcKeyPair(), destDir, false);
                    PreferenceManager.getInstance().putString(PRIVATE_KEY_PATH, new File(destDir, fileName).getAbsolutePath());

                    if (callback != null) {
                        callback.onCompleted(true);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "import wallet failed. e = " + e.getMessage());

                    if (callback != null) {
                        callback.onCompleted(false);
                    }
                }
            }
        }).start();
    }

    public Wallet getWallet() {
        if (mWallet == null) {
            mWallet = Wallet.load();
        }
        return mWallet;
    }

    public boolean walletExist() {
        String path = PreferenceManager.getInstance().getString(PRIVATE_KEY_PATH);
        if (!TextUtils.isEmpty(path) && new File(path).exists()) {
            return true;
        }
        return false;
    }

    public Credentials getCredentials() {
        return mCredentials;
    }

    public Credentials loadCredentials(String password) {
        String path = PreferenceManager.getInstance().getString(PRIVATE_KEY_PATH);
        try {
            mCredentials = WalletUtils.loadCredentials(password, new File(path));
            return mCredentials;
        } catch (Exception e) {
        }
        return null;
    }

    private WalletManager() {
    }
}
