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

package org.arpnetwork.arpdevice.ui.miner;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import org.arpnetwork.arpdevice.contracts.ARPContract;
import org.arpnetwork.arpdevice.contracts.ARPRegistry;
import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.contracts.api.TransactionAPI;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;

import static org.arpnetwork.arpdevice.config.Constant.KEY_ADDRESS;
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASLIMIT;
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASPRICE;
import static org.arpnetwork.arpdevice.config.Constant.KEY_OP;
import static org.arpnetwork.arpdevice.config.Constant.KEY_PASSWD;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerFragment.OPERATION_APPROVE;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerFragment.OPERATION_BIND;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerFragment.OPERATION_UNBIND;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_APPROVE_FAILED;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_APPROVE_SUCCESS;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_APPROVE_RUNNING;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BIND_FAILED;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BIND_RUNNING;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BIND_SUCCESS;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_UNBIND_FAILED;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_UNBIND_RUNNING;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_UNBIND_SUCCESS;

public class BindMinerIntentService extends IntentService {
    private static final String TAG = "BindMinerIntentService";
    private BroadcastNotifier mBroadcaster = new BroadcastNotifier(this);

    public BindMinerIntentService() {
        super("BindMinerIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        int type = intent.getExtras().getInt(KEY_OP);
        String password = intent.getExtras().getString(KEY_PASSWD);
        String gasPrice = intent.getExtras().getString(KEY_GASPRICE);
        String gasLimit = intent.getExtras().getString(KEY_GASLIMIT);

        if (type == OPERATION_UNBIND) {
            mBroadcaster.broadcastWithState(STATE_UNBIND_RUNNING, type, null);
            boolean result = unbindDevice(Wallet.loadCredentials(password),
                    new BigInteger(gasPrice), new BigInteger(gasLimit));
            if (result) {
                mBroadcaster.broadcastWithState(STATE_UNBIND_SUCCESS, type, null);
            } else {
                mBroadcaster.broadcastWithState(STATE_UNBIND_FAILED, type, null);
            }
        } else if (type == OPERATION_BIND) {
            String address = intent.getExtras().getString(KEY_ADDRESS);

            mBroadcaster.broadcastWithState(STATE_BIND_RUNNING, type, address);

            boolean result = bindDevice(address, Wallet.loadCredentials(password),
                    new BigInteger(gasPrice), new BigInteger(gasLimit));
            if (result) {
                mBroadcaster.broadcastWithState(STATE_BIND_SUCCESS, type, address);
            } else {
                mBroadcaster.broadcastWithState(STATE_BIND_FAILED, type, address);
            }
        } else if (type == OPERATION_APPROVE) {
            mBroadcaster.broadcastWithState(STATE_APPROVE_RUNNING, type, null);

            boolean result = approve(Wallet.loadCredentials(password), new BigInteger(gasPrice));
            if (result) {
                mBroadcaster.broadcastWithState(STATE_APPROVE_SUCCESS, type, null);
            } else {
                mBroadcaster.broadcastWithState(STATE_APPROVE_FAILED, type, null);
            }
        }
    }

    private boolean approve(Credentials credentials, BigInteger gasPrice) {
        boolean result;
        BigInteger gasLimit;
        try {
            gasLimit = TransactionAPI.getTransactionGasLimit(ARPContract.getApproveEstimateGasTrans());
        } catch (IOException e) {
            gasLimit = new BigInteger("400000");
        }
        String hexData = ARPContract.getTransactionHexData(ARPRegistry.CONTRACT_ADDRESS,
                credentials, gasPrice, gasLimit);
        try {
            EtherAPI.getWeb3J().ethSendRawTransaction(hexData).send();
            result = true;
        } catch (IOException e) {
            result = false;
        }
        return result;
    }

    private boolean bindDevice(String address, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        boolean success = false;
        ARPRegistry registry = ARPRegistry.load(ARPRegistry.CONTRACT_ADDRESS, EtherAPI.getWeb3J(),
                credentials, gasPrice, gasLimit);
        try {
            TransactionReceipt bindDeviceReceipt = registry.bindDevice(address).send();
            success = isStatusOK(bindDeviceReceipt.getStatus());
        } catch (Exception e) {
        }
        return success;
    }

    private boolean unbindDevice(Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        boolean success = false;
        ARPRegistry registry = ARPRegistry.load(ARPRegistry.CONTRACT_ADDRESS, EtherAPI.getWeb3J(),
                credentials, gasPrice, gasLimit);
        try {
            TransactionReceipt bindDeviceReceipt = registry.unbindDevice().send();
            success = isStatusOK(bindDeviceReceipt.getStatus());
        } catch (Exception e) {
        }
        return success;
    }

    public boolean isStatusOK(String status) {
        if (null == status) {
            return true;
        }
        BigInteger statusQuantity = Numeric.decodeQuantity(status);
        return BigInteger.ONE.equals(statusQuantity);
    }
}
