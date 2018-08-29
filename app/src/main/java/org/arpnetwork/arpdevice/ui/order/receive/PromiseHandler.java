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

package org.arpnetwork.arpdevice.ui.order.receive;

import android.text.TextUtils;

import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.contracts.api.VerifyAPI;
import org.arpnetwork.arpdevice.data.BankAllowance;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.miner.BindMinerHelper;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

public class PromiseHandler {
    private Miner mMiner;
    private OnReceivePromiseListener mOnReceivePromiseListener;

    public interface OnReceivePromiseListener {
        void onReceivePromise(Promise promise);
    }

    public PromiseHandler(OnReceivePromiseListener listener, Miner miner) {
        mOnReceivePromiseListener = listener;
        mMiner = miner;
    }

    public boolean processPromise(String promiseJson) {
        Promise promise = Promise.fromJson(promiseJson);
        if (checkPromise(promise)) {
            promise.save();

            if (mOnReceivePromiseListener != null) {
                mOnReceivePromiseListener.onReceivePromise(promise);
            }
            return true;
        }
        return false;
    }

    private boolean checkPromise(Promise promise) {
        boolean res = false;
        String walletAddr = Wallet.get().getAddress();
        BankAllowance allowance = BankAllowance.get();
        Promise lastPromise = Promise.get();
        BigInteger lastAmount = BigInteger.ZERO;
        if (lastPromise != null) {
            lastAmount = new BigInteger(lastPromise.getAmount(), 16);
        }

        if (!TextUtils.isEmpty(promise.getCid())
                && !TextUtils.isEmpty(promise.getFrom())
                && !TextUtils.isEmpty(promise.getTo())
                && !TextUtils.isEmpty(promise.getAmount())
                && new BigInteger(promise.getCid(), 16).compareTo(allowance.id) == 0
                && Numeric.cleanHexPrefix(promise.getFrom()).equals(Numeric.cleanHexPrefix(mMiner.getAddress()))
                && Numeric.cleanHexPrefix(promise.getTo()).equals(Numeric.cleanHexPrefix(walletAddr))
                && VerifyAPI.isEffectivePromise(promise)
                && new BigInteger(promise.getAmount(), 16).compareTo(allowance.amount) <= 0
                && new BigInteger(promise.getAmount(), 16).compareTo(lastAmount) > 0) {
            res = true;
        }
        return res;
    }
}
