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

import org.arpnetwork.arpdevice.util.PreferenceManager;

public class Wallet {
    private static final String NAME = "name";
    private static final String PUBLIC_KEY = "public_key";

    private String name;
    private String publicKey;
    private String privateKey;

    public Wallet() {
    }

    public Wallet(String name, String privateKey) {
        this.name = name;
        this.privateKey = privateKey;
    }

    public String getName() {
        return name;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void save() {
        PreferenceManager.getInstance().putString(NAME, name);
        PreferenceManager.getInstance().putString(PUBLIC_KEY, publicKey);
    }

    public static Wallet load() {
        Wallet wallet = new Wallet();
        wallet.name = PreferenceManager.getInstance().getString(NAME);
        wallet.publicKey = PreferenceManager.getInstance().getString(PUBLIC_KEY);
        return wallet;
    }
}
