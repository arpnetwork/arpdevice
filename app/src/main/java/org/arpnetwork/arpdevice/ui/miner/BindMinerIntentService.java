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
import android.util.Log;

import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.contracts.ARPContract;
import org.arpnetwork.arpdevice.contracts.ARPRegistry;
import org.arpnetwork.arpdevice.contracts.api.TransactionAPI;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.ui.bean.BindPromise;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;

import java.math.BigInteger;

import static org.arpnetwork.arpdevice.config.Constant.KEY_ADDRESS;
import static org.arpnetwork.arpdevice.config.Constant.KEY_BINDPROMISE;
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASLIMIT;
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASPRICE;
import static org.arpnetwork.arpdevice.config.Constant.KEY_OP;
import static org.arpnetwork.arpdevice.config.Constant.KEY_PASSWD;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_APPROVE_FAILED;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_APPROVE_SUCCESS;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_APPROVE_RUNNING;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BANK_APPROVE_FAILED;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BANK_APPROVE_RUNNING;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BANK_APPROVE_SUCCESS;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BANK_CANCEL_APPROVE_FAILED;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BANK_CANCEL_APPROVE_RUNNING;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BANK_CANCEL_APPROVE_SUCCESS;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BIND_FAILED;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BIND_RUNNING;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BIND_SUCCESS;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_DEPOSIT_FAILED;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_DEPOSIT_RUNNING;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_DEPOSIT_SUCCESS;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_UNBIND_FAILED;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_UNBIND_RUNNING;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_UNBIND_SUCCESS;

public class BindMinerIntentService extends IntentService {
    private static final String TAG = "BindMinerIntentService";

    public static final int OPERATION_ARP_APPROVE = 1;
    public static final int OPERATION_BANK_APPROVE = 2;
    public static final int OPERATION_BANK_DEPOSIT = 3;
    public static final int OPERATION_BIND = 4;
    public static final int OPERATION_UNBIND = 5;
    public static final int OPERATION_CANCEL_APPROVE = 6;

    private BroadcastNotifier mBroadcaster = new BroadcastNotifier(this);

    public BindMinerIntentService() {
        super("BindMinerIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        final int type = intent.getExtras().getInt(KEY_OP);
        String password = intent.getExtras().getString(KEY_PASSWD);
        BigInteger gasPrice = new BigInteger(intent.getExtras().getString(KEY_GASPRICE));
        BigInteger gasLimit = new BigInteger(intent.getExtras().getString(KEY_GASLIMIT));

        switch (type) {
            case OPERATION_UNBIND: {
                String address = intent.getExtras().getString(KEY_ADDRESS);
                mBroadcaster.broadcastWithState(STATE_UNBIND_RUNNING, type, address);

                boolean result = false;
                try {
                    result = unbindDevice(Wallet.loadCredentials(password), gasPrice, gasLimit);
                } catch (Exception e) {
                    Log.e(TAG, "unbind device error:" + e.getCause());
                    mBroadcaster.broadcastWithState(STATE_UNBIND_FAILED, type, null);
                    break;
                }

                mBroadcaster.broadcastWithState(result ? STATE_UNBIND_SUCCESS : STATE_UNBIND_FAILED, type, address);
                break;
            }

            case OPERATION_BIND: {
                String address = intent.getExtras().getString(KEY_ADDRESS);
                mBroadcaster.broadcastWithState(STATE_BIND_RUNNING, type, address);

                boolean result = false;
                BindPromise bindPromise = (BindPromise) intent.getSerializableExtra(KEY_BINDPROMISE);
                try {
                    result = bindDevice(address, bindPromise, Wallet.loadCredentials(password),
                            gasPrice, gasLimit);
                } catch (Exception e) {
                    Log.e(TAG, "bind device error:" + e.getCause());
                    mBroadcaster.broadcastWithState(STATE_BIND_FAILED, type, null);
                    break;
                }

                mBroadcaster.broadcastWithState(result ? STATE_BIND_SUCCESS : STATE_BIND_FAILED, type, address);
                break;
            }

            case OPERATION_ARP_APPROVE: {
                mBroadcaster.broadcastWithState(STATE_APPROVE_RUNNING, type, null);

                boolean result = false;
                try {
                    result = arpApprove(Wallet.loadCredentials(password), gasPrice, gasLimit);
                } catch (Exception e) {
                    Log.e(TAG, "arpApprove, error:" + e.getCause());
                    mBroadcaster.broadcastWithState(STATE_APPROVE_FAILED, type, null);
                    break;
                }

                mBroadcaster.broadcastWithState(result ? STATE_APPROVE_SUCCESS : STATE_APPROVE_FAILED, type, null);
                break;
            }

            case OPERATION_BANK_APPROVE: {
                mBroadcaster.broadcastWithState(STATE_BANK_APPROVE_RUNNING, type, null);

                boolean result = false;
                try {
                    result = bankApprove(Wallet.loadCredentials(password), gasPrice, gasLimit);
                } catch (Exception e) {
                    Log.e(TAG, "bank approve error:" + e.getCause());
                    mBroadcaster.broadcastWithState(STATE_BANK_APPROVE_FAILED, type, null);
                    break;
                }

                mBroadcaster.broadcastWithState(result ? STATE_BANK_APPROVE_SUCCESS : STATE_BANK_APPROVE_FAILED, type, null);
                break;
            }

            case OPERATION_BANK_DEPOSIT: {
                mBroadcaster.broadcastWithState(STATE_DEPOSIT_RUNNING, type, null);

                boolean result = false;
                try {
                    result = deposit(Wallet.loadCredentials(password), gasPrice, gasLimit);
                } catch (Exception e) {
                    Log.e(TAG, "deposit error:" + e.getCause());
                    mBroadcaster.broadcastWithState(STATE_DEPOSIT_FAILED, type, null);
                    break;
                }

                mBroadcaster.broadcastWithState(result ? STATE_DEPOSIT_SUCCESS : STATE_DEPOSIT_FAILED, type, null);
                break;
            }

            case OPERATION_CANCEL_APPROVE: {
                mBroadcaster.broadcastWithState(STATE_BANK_CANCEL_APPROVE_RUNNING, type, null);

                boolean result = false;
                String address = intent.getExtras().getString(KEY_ADDRESS);
                try {
                    result = cancelApprove(Wallet.loadCredentials(password), address, gasPrice, gasLimit);
                } catch (Exception e) {
                    Log.e(TAG, "cancel approval by spender error:" + e.getCause());
                    mBroadcaster.broadcastWithState(STATE_BANK_CANCEL_APPROVE_FAILED, type, null);
                    break;
                }

                mBroadcaster.broadcastWithState(result ? STATE_BANK_CANCEL_APPROVE_SUCCESS : STATE_BANK_CANCEL_APPROVE_FAILED, type, null);
                break;
            }

            default:
                break;
        }
    }

    private boolean unbindDevice(Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) throws Exception {
        ARPRegistry registry = ARPRegistry.load(ARPRegistry.CONTRACT_ADDRESS,
                credentials, gasPrice, gasLimit);

        TransactionReceipt unbindDeviceReceipt = registry.unbindDevice().send();

        Boolean result = TransactionAPI.isStatusOK(unbindDeviceReceipt.getStatus());
        if (result) {
            Promise.clear();

            StateHolder.clearAllState();
            CustomApplication.sInstance.stopMonitorService();
        }
        return result;
    }

    private boolean bindDevice(String address, BindPromise bindPromise, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) throws Exception {
        ARPRegistry registry = ARPRegistry.load(ARPRegistry.CONTRACT_ADDRESS,
                credentials, gasPrice, gasLimit);

        TransactionReceipt bindDeviceReceipt = registry.bindDevice(address, bindPromise.getAmount(),
                bindPromise.getExpired(), bindPromise.getSignExpired(),
                new BigInteger(String.valueOf(bindPromise.getSignatureData().getV())),
                bindPromise.getSignatureData().getR(), bindPromise.getSignatureData().getS()).send();

        boolean result = TransactionAPI.isStatusOK(bindDeviceReceipt.getStatus());
        if (result) {
            CustomApplication.sInstance.startMonitorService();
        }
        return result;
    }

    private boolean arpApprove(Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) throws Exception {
        ARPContract contract = ARPContract.load(credentials, gasPrice, gasLimit);
        TransactionReceipt receipt = contract.approve().send();
        return TransactionAPI.isStatusOK(receipt.getStatus());
    }

    private boolean bankApprove(Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) throws Exception {
        ARPBank bank = ARPBank.load(credentials, gasPrice, gasLimit);
        TransactionReceipt receipt = bank.approve(ARPRegistry.CONTRACT_ADDRESS, BigInteger.ZERO, "0").send();
        return TransactionAPI.isStatusOK(receipt.getStatus());
    }

    private boolean deposit(Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) throws Exception {
        ARPBank bank = ARPBank.load(credentials, gasPrice, gasLimit);
        TransactionReceipt receipt = bank.deposit(new BigInteger(Convert.toWei(ARPBank.DEPOSIT_ARP_NUMBER, Convert.Unit.ETHER).toString())).send();
        return TransactionAPI.isStatusOK(receipt.getStatus());
    }

    private boolean cancelApprove(Credentials credentials, String address, BigInteger gasPrice, BigInteger gasLimit) throws Exception {
        ARPBank bank = ARPBank.load(credentials, gasPrice, gasLimit);
        TransactionReceipt receipt = bank.cancelApprovalBySpender(address).send();
        return TransactionAPI.isStatusOK(receipt.getStatus());
    }
}
