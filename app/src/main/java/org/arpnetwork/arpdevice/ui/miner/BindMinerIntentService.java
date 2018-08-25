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
import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.contracts.api.TransactionAPI;
import org.arpnetwork.arpdevice.contracts.api.VerifyAPI;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.ui.bean.BindPromise;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.spongycastle.util.encoders.Hex;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigInteger;

import static org.arpnetwork.arpdevice.config.Constant.KEY_ADDRESS;
import static org.arpnetwork.arpdevice.config.Constant.KEY_BINDPROMISE;
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASLIMIT;
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASPRICE;
import static org.arpnetwork.arpdevice.config.Constant.KEY_OP;
import static org.arpnetwork.arpdevice.config.Constant.KEY_PASSWD;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerFragment.OPERATION_ARP_APPROVE;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerFragment.OPERATION_BANK_APPROVE;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerFragment.OPERATION_BANK_DEPOSIT;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerFragment.OPERATION_BIND;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerFragment.OPERATION_CANCEL_APPROVE;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerFragment.OPERATION_UNBIND;
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
    private static final String DEPOSIT_ARP_NUMBER = "500";

    private BroadcastNotifier mBroadcaster = new BroadcastNotifier(this);

    public BindMinerIntentService() {
        super("BindMinerIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        final int type = intent.getExtras().getInt(KEY_OP);
        String password = intent.getExtras().getString(KEY_PASSWD);
        String gasPrice = intent.getExtras().getString(KEY_GASPRICE);
        String gasLimit = intent.getExtras().getString(KEY_GASLIMIT);

        switch (type) {
            case OPERATION_UNBIND: {
                String address = intent.getExtras().getString(KEY_ADDRESS);
                mBroadcaster.broadcastWithState(STATE_UNBIND_RUNNING, type, address);
                boolean result = unbindDevice(Wallet.loadCredentials(password),
                        new BigInteger(gasPrice), new BigInteger(gasLimit));
                if (result) {
                    Promise.clear();

                    StateHolder.clearAllState();
                    CustomApplication.sInstance.stopMonitorService();

                    mBroadcaster.broadcastWithState(STATE_UNBIND_SUCCESS, type, address);
                } else {
                    mBroadcaster.broadcastWithState(STATE_UNBIND_FAILED, type, address);
                }
                break;
            }

            case OPERATION_BIND: {
                String address = intent.getExtras().getString(KEY_ADDRESS);
                BindPromise bindPromise = (BindPromise) intent.getSerializableExtra(KEY_BINDPROMISE);

                mBroadcaster.broadcastWithState(STATE_BIND_RUNNING, type, address);

                boolean result = bindDevice(address, bindPromise, Wallet.loadCredentials(password),
                        new BigInteger(gasPrice), new BigInteger(gasLimit));
                if (result) {
                    CustomApplication.sInstance.startMonitorService();

                    mBroadcaster.broadcastWithState(STATE_BIND_SUCCESS, type, address);
                } else {
                    mBroadcaster.broadcastWithState(STATE_BIND_FAILED, type, address);
                }
                break;
            }

            // FIXME: refactor
            case OPERATION_ARP_APPROVE: {
                mBroadcaster.broadcastWithState(STATE_APPROVE_RUNNING, type, null);

                boolean result = arpApprove(Wallet.loadCredentials(password), new BigInteger(gasPrice));
                if (result) {
                    mBroadcaster.broadcastWithState(STATE_APPROVE_SUCCESS, type, null);
                } else {
                    mBroadcaster.broadcastWithState(STATE_APPROVE_FAILED, type, null);
                }
                break;
            }

            case OPERATION_BANK_APPROVE: {
                mBroadcaster.broadcastWithState(STATE_BANK_APPROVE_RUNNING, type, null);

                ARPBank bank = ARPBank.load(Wallet.loadCredentials(password), new BigInteger(gasPrice), new BigInteger(gasLimit));
                TransactionReceipt receipt = null;
                try {
                    receipt = bank.approve(ARPRegistry.CONTRACT_ADDRESS, BigInteger.ZERO, "0").send();
                } catch (Exception e) {
                    Log.e(TAG, "approve error:" + e.getCause());
                }

                boolean success = TransactionAPI.isStatusOK(receipt.getStatus());
                if (success) {
                    mBroadcaster.broadcastWithState(STATE_BANK_APPROVE_SUCCESS, type, null);
                } else {
                    mBroadcaster.broadcastWithState(STATE_BANK_APPROVE_FAILED, type, null);
                }
                break;
            }

            case OPERATION_BANK_DEPOSIT: {
                mBroadcaster.broadcastWithState(STATE_DEPOSIT_RUNNING, type, null);

                ARPBank bank = ARPBank.load(Wallet.loadCredentials(password), new BigInteger(gasPrice), new BigInteger(gasLimit));
                TransactionReceipt receipt = null;
                try {
                    receipt = bank.deposit(new BigInteger(Convert.toWei(DEPOSIT_ARP_NUMBER, Convert.Unit.ETHER).toString())).send();
                } catch (Exception e) {
                    Log.e(TAG, "deposit error:" + e.getCause());
                }

                boolean success = TransactionAPI.isStatusOK(receipt.getStatus());
                if (success) {
                    mBroadcaster.broadcastWithState(STATE_DEPOSIT_SUCCESS, type, null);
                } else {
                    mBroadcaster.broadcastWithState(STATE_DEPOSIT_FAILED, type, null);
                }
                break;
            }

            case OPERATION_CANCEL_APPROVE: {
                String address = intent.getExtras().getString(KEY_ADDRESS);
                mBroadcaster.broadcastWithState(STATE_BANK_CANCEL_APPROVE_RUNNING, type, null);

                ARPBank bank = ARPBank.load(Wallet.loadCredentials(password), new BigInteger(gasPrice), new BigInteger(gasLimit));
                TransactionReceipt receipt = null;
                try {
                    receipt = bank.cancelApprovalBySpender(address).send();
                } catch (Exception e) {
                    Log.e(TAG, "cancel approval by spender error:" + e.getCause());
                }

                boolean success = TransactionAPI.isStatusOK(receipt.getStatus());
                if (success) {
                    mBroadcaster.broadcastWithState(STATE_BANK_CANCEL_APPROVE_SUCCESS, type, null);
                } else {
                    mBroadcaster.broadcastWithState(STATE_BANK_CANCEL_APPROVE_FAILED, type, null);
                }
                break;
            }

            default:
                break;
        }
    }

    private boolean arpApprove(Credentials credentials, BigInteger gasPrice) {
        boolean result;
        BigInteger gasLimit;
        try {
            gasLimit = TransactionAPI.getTransactionGasLimit(ARPContract.getApproveEstimateGasTrans());
        } catch (IOException e) {
            gasLimit = new BigInteger("400000");
        }
        String hexData = ARPContract.getTransactionHexData(ARPBank.CONTRACT_ADDRESS,
                credentials, gasPrice, gasLimit);
        try {
            EtherAPI.getWeb3J().ethSendRawTransaction(hexData).send();
            result = true;
        } catch (IOException e) {
            result = false;
        }
        return result;
    }

    private boolean bindDevice(String address, BindPromise bindPromise, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        boolean success = false;
        ARPRegistry registry = ARPRegistry.load(ARPRegistry.CONTRACT_ADDRESS, EtherAPI.getWeb3J(),
                credentials, gasPrice, gasLimit);
        try {
            byte[] signatureDataBytes = Hex.decode(bindPromise.getPromiseSign());
            Sign.SignatureData signatureData = VerifyAPI.getSignatureDataFromByte(signatureDataBytes);

            TransactionReceipt bindDeviceReceipt = registry.bindDevice(address, bindPromise.getAmount(), bindPromise.getExpired(), bindPromise.getSignExpired(), new BigInteger(String.valueOf(signatureData.getV())), signatureData.getR(), signatureData.getS()).send();
            success = TransactionAPI.isStatusOK(bindDeviceReceipt.getStatus());
        } catch (Exception e) {
        }
        return success;
    }

    private boolean unbindDevice(Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        boolean success = false;
        ARPRegistry registry = ARPRegistry.load(ARPRegistry.CONTRACT_ADDRESS, EtherAPI.getWeb3J(),
                credentials, gasPrice, gasLimit);
        try {
            TransactionReceipt unbindDeviceReceipt = registry.unbindDevice().send();
            success = TransactionAPI.isStatusOK(unbindDeviceReceipt.getStatus());
        } catch (Exception e) {
        }
        return success;
    }


}
