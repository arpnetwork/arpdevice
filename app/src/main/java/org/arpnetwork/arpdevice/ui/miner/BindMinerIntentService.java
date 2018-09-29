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
import android.text.TextUtils;
import android.util.Log;

import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.contracts.ARPContract;
import org.arpnetwork.arpdevice.contracts.ARPRegistry;
import org.arpnetwork.arpdevice.contracts.api.CustomRawTransactionManager;
import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.contracts.api.TransactionAPI;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.database.EarningRecord;
import org.arpnetwork.arpdevice.database.TransactionRecord;
import org.arpnetwork.arpdevice.ui.bean.BindPromise;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;

import java.math.BigInteger;

import static org.arpnetwork.arpdevice.config.Config.DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH;
import static org.arpnetwork.arpdevice.config.Config.DEFAULT_POLLING_FREQUENCY;
import static org.arpnetwork.arpdevice.config.Constant.KEY_ADDRESS;
import static org.arpnetwork.arpdevice.config.Constant.KEY_BINDPROMISE;
import static org.arpnetwork.arpdevice.config.Constant.KEY_EXCHANGE_AMOUNT;
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASLIMIT;
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASPRICE;
import static org.arpnetwork.arpdevice.config.Constant.KEY_OP;
import static org.arpnetwork.arpdevice.config.Constant.KEY_PASSWD;
import static org.arpnetwork.arpdevice.config.Constant.KEY_TX_HASH;
import static org.arpnetwork.arpdevice.contracts.ARPBank.DEPOSIT_ARP_NUMBER;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_APPROVE_FAILED;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_APPROVE_SUCCESS;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_APPROVE_RUNNING;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BANK_APPROVE_FAILED;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BANK_APPROVE_RUNNING;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BANK_APPROVE_SUCCESS;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BANK_CASH_FAILED;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BANK_CASH_RUNNING;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BANK_CASH_SUCCESS;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BANK_WITHDRAW_FAILED;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BANK_WITHDRAW_RUNNING;
import static org.arpnetwork.arpdevice.ui.miner.StateHolder.STATE_BANK_WITHDRAW_SUCCESS;
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
    public static final int OPERATION_CASH = 6;
    public static final int OPERATION_WITHDRAW = 7;

    private BroadcastNotifier mBroadcaster = new BroadcastNotifier();

    public BindMinerIntentService() {
        super("BindMinerIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String dbTxHash = intent.getExtras().getString(KEY_TX_HASH);
        int type = intent.getExtras().getInt(KEY_OP);

        String password = null;
        BigInteger gasPrice = null;
        BigInteger gasLimit = null;
        CustomRawTransactionManager transactionManager = null;

        if (TextUtils.isEmpty(dbTxHash)) {
            password = intent.getExtras().getString(KEY_PASSWD);
            gasPrice = new BigInteger(intent.getExtras().getString(KEY_GASPRICE));
            gasLimit = new BigInteger(intent.getExtras().getString(KEY_GASLIMIT));

            Credentials credentials = Wallet.loadCredentials(password);
            transactionManager = new CustomRawTransactionManager(EtherAPI.getWeb3J(), credentials,
                    DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH, DEFAULT_POLLING_FREQUENCY); // 1 hour waiting.
        }

        switch (type) {
            case OPERATION_UNBIND: {
                String address = intent.getExtras().getString(KEY_ADDRESS);
                mBroadcaster.broadcastWithState(STATE_UNBIND_RUNNING, type, address);

                boolean result = false;
                try {
                    if (!TextUtils.isEmpty(dbTxHash)) {
                        TransactionReceipt receipt = TransactionAPI.pollingTransaction(dbTxHash);
                        result = TransactionAPI.isStatusOK(receipt.getStatus());

                        deleteHash(dbTxHash, OPERATION_UNBIND);
                    } else {
                        HashBackListener hashBackListener = new HashBackListener(OPERATION_UNBIND, address);
                        transactionManager.setListener(hashBackListener);
                        result = unbindDevice(transactionManager, gasPrice, gasLimit);

                        String hash = hashBackListener.getTxHash();
                        deleteHash(hash, OPERATION_UNBIND);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "unbind device error:" + e.getCause());
                    mBroadcaster.broadcastWithState(STATE_UNBIND_FAILED, type, address);
                    break;
                }

                if (result) {
                    Promise.clear();

                    StateHolder.clearAllState();
                    CustomApplication.sInstance.stopMonitorService();
                }
                mBroadcaster.broadcastWithState(result ? STATE_UNBIND_SUCCESS : STATE_UNBIND_FAILED, type, address);
                break;
            }

            case OPERATION_BIND: {
                String address = intent.getExtras().getString(KEY_ADDRESS);
                mBroadcaster.broadcastWithState(STATE_BIND_RUNNING, type, address);

                boolean result = false;
                try {
                    if (!TextUtils.isEmpty(dbTxHash)) {
                        TransactionReceipt receipt = TransactionAPI.pollingTransaction(dbTxHash);
                        result = TransactionAPI.isStatusOK(receipt.getStatus());

                        deleteHash(dbTxHash, OPERATION_BIND);
                    } else {
                        BindPromise bindPromise = (BindPromise) intent.getSerializableExtra(KEY_BINDPROMISE);
                        HashBackListener hashBackListener = new HashBackListener(OPERATION_BIND, address);
                        transactionManager.setListener(hashBackListener);
                        result = bindDevice(address, bindPromise, transactionManager,
                                gasPrice, gasLimit);

                        String hash = hashBackListener.getTxHash();
                        deleteHash(hash, OPERATION_BIND);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "bind device error:" + e.getCause());
                    mBroadcaster.broadcastWithState(STATE_BIND_FAILED, type, address);
                    break;
                }

                if (result) {
                    CustomApplication.sInstance.startMonitorService();
                }
                mBroadcaster.broadcastWithState(result ? STATE_BIND_SUCCESS : STATE_BIND_FAILED, type, address);
                break;
            }

            case OPERATION_ARP_APPROVE: {
                mBroadcaster.broadcastWithState(STATE_APPROVE_RUNNING, type, null);

                boolean result = false;
                try {
                    if (!TextUtils.isEmpty(dbTxHash)) {
                        TransactionReceipt receipt = TransactionAPI.pollingTransaction(dbTxHash);
                        result = TransactionAPI.isStatusOK(receipt.getStatus());

                        deleteHash(dbTxHash, OPERATION_ARP_APPROVE);
                    } else {
                        HashBackListener hashBackListener = new HashBackListener(OPERATION_ARP_APPROVE);
                        transactionManager.setListener(hashBackListener);
                        result = arpApprove(transactionManager, gasPrice, gasLimit);

                        String hash = hashBackListener.getTxHash();
                        deleteHash(hash, OPERATION_ARP_APPROVE);
                    }
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
                    if (!TextUtils.isEmpty(dbTxHash)) {
                        TransactionReceipt receipt = TransactionAPI.pollingTransaction(dbTxHash);
                        result = TransactionAPI.isStatusOK(receipt.getStatus());

                        deleteHash(dbTxHash, OPERATION_BANK_APPROVE);
                    } else {
                        HashBackListener hashBackListener = new HashBackListener(OPERATION_BANK_APPROVE);
                        transactionManager.setListener(hashBackListener);
                        result = bankApprove(transactionManager, gasPrice, gasLimit);

                        String hash = hashBackListener.getTxHash();
                        deleteHash(hash, OPERATION_BANK_APPROVE);
                    }
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
                    if (!TextUtils.isEmpty(dbTxHash)) {
                        TransactionReceipt receipt = TransactionAPI.pollingTransaction(dbTxHash);
                        result = TransactionAPI.isStatusOK(receipt.getStatus());

                        deleteHash(dbTxHash, OPERATION_BANK_DEPOSIT);
                    } else {
                        HashBackListener hashBackListener = new HashBackListener(OPERATION_BANK_DEPOSIT);
                        transactionManager.setListener(hashBackListener);
                        result = deposit(transactionManager, gasPrice, gasLimit);

                        String hash = hashBackListener.getTxHash();
                        deleteHash(hash, OPERATION_BANK_DEPOSIT);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "deposit error:" + e.getCause());
                    mBroadcaster.broadcastWithState(STATE_DEPOSIT_FAILED, type, null);
                    break;
                }

                mBroadcaster.broadcastWithState(result ? STATE_DEPOSIT_SUCCESS : STATE_DEPOSIT_FAILED, type, null);
                break;
            }

            case OPERATION_CASH: {
                mBroadcaster.broadcastWithState(STATE_BANK_CASH_RUNNING, type, null);

                final BigInteger amount = new BigInteger(intent.getExtras().getString(KEY_EXCHANGE_AMOUNT));
                final Promise promise = Promise.get();
                String address = Wallet.get().getAddress();

                boolean result = false;
                try {
                    if (!TextUtils.isEmpty(dbTxHash)) {
                        TransactionReceipt receipt = TransactionAPI.pollingTransaction(dbTxHash);
                        result = TransactionAPI.isStatusOK(receipt.getStatus());

                        deleteHash(dbTxHash, OPERATION_CASH);
                    } else {
                        HashBackListener hashBackListener = new HashBackListener(OPERATION_CASH, amount);
                        transactionManager.setListener(hashBackListener);
                        result = cash(promise, address, transactionManager, gasPrice, gasLimit);

                        String hash = hashBackListener.getTxHash();
                        deleteHash(hash, OPERATION_CASH);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "cash error:" + e.getCause());
                    mBroadcaster.broadcastWithState(STATE_BANK_CASH_FAILED, type, null);
                    break;
                }

                mBroadcaster.broadcastWithState(result ? STATE_BANK_CASH_SUCCESS : STATE_BANK_CASH_FAILED, type, null);
                break;
            }

            case OPERATION_WITHDRAW: {
                mBroadcaster.broadcastWithState(STATE_BANK_WITHDRAW_RUNNING, type, null);

                BigInteger amount;
                String amountString = intent.getExtras().getString(KEY_EXCHANGE_AMOUNT);
                if (amountString != null) {
                    amount = new BigInteger(amountString);
                } else {
                    amount = Convert.toWei(DEPOSIT_ARP_NUMBER, Convert.Unit.ETHER).toBigInteger();
                }

                boolean result = false;
                try {
                    if (!TextUtils.isEmpty(dbTxHash)) {
                        TransactionReceipt receipt = TransactionAPI.pollingTransaction(dbTxHash);
                        result = TransactionAPI.isStatusOK(receipt.getStatus());

                        deleteHash(dbTxHash, OPERATION_WITHDRAW);
                    } else {
                        HashBackListener hashBackListener = new HashBackListener(OPERATION_WITHDRAW);
                        transactionManager.setListener(hashBackListener);
                        result = withdraw(amount, transactionManager, gasPrice, gasLimit);

                        String hash = hashBackListener.getTxHash();
                        deleteHash(hash, OPERATION_WITHDRAW);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "withdraw error:" + e.getCause());
                    mBroadcaster.broadcastWithState(STATE_BANK_WITHDRAW_FAILED, type, null);
                    break;
                }

                mBroadcaster.broadcastWithState(result ? STATE_BANK_WITHDRAW_SUCCESS : STATE_BANK_WITHDRAW_FAILED, type, null);
                break;
            }

            default:
                break;
        }
    }

    private void savePendingToDb(String transactionHash, BigInteger earning) {
        final EarningRecord localRecord = new EarningRecord();
        localRecord.state = EarningRecord.STATE_PENDING;
        localRecord.time = System.currentTimeMillis();
        localRecord.earning = earning.toString();
        localRecord.setKey(transactionHash);
        localRecord.minerAddress = Promise.get().getFrom();
        localRecord.saveRecord();
    }

    private void saveHashToDb(String transactionHash, int opType, String args) {
        TransactionRecord record = TransactionRecord.get(transactionHash);
        if (record != null) {
            record.opType = opType;
            record.args = args;
            record.saveRecord();
        }
    }

    private void deleteHash(String transactionHash, int opType) {
        TransactionRecord.delete(transactionHash, opType);
    }

    private boolean unbindDevice(CustomRawTransactionManager manager, BigInteger gasPrice, BigInteger gasLimit) throws Exception {
        ARPRegistry registry = ARPRegistry.load(manager, gasPrice, gasLimit);
        TransactionReceipt receipt = registry.unbindDevice().send();
        return TransactionAPI.isStatusOK(receipt.getStatus());
    }

    private boolean bindDevice(String address, BindPromise bindPromise, CustomRawTransactionManager manager, BigInteger gasPrice, BigInteger gasLimit) throws Exception {
        ARPRegistry registry = ARPRegistry.load(manager, gasPrice, gasLimit);

        TransactionReceipt receipt = registry.bindDevice(address, bindPromise.getAmount(),
                bindPromise.getExpired(), bindPromise.getSignExpired(),
                new BigInteger(String.valueOf(bindPromise.getSignatureData().getV())),
                bindPromise.getSignatureData().getR(), bindPromise.getSignatureData().getS()).send();

        return TransactionAPI.isStatusOK(receipt.getStatus());
    }

    private boolean arpApprove(CustomRawTransactionManager manager, BigInteger gasPrice, BigInteger gasLimit) throws Exception {
        ARPContract contract = ARPContract.load(manager, gasPrice, gasLimit);
        TransactionReceipt receipt = contract.approve().send();
        return TransactionAPI.isStatusOK(receipt.getStatus());
    }

    private boolean bankApprove(CustomRawTransactionManager manager, BigInteger gasPrice, BigInteger gasLimit) throws Exception {
        ARPBank bank = ARPBank.load(manager, gasPrice, gasLimit);
        TransactionReceipt receipt = bank.approve(ARPRegistry.CONTRACT_ADDRESS, BigInteger.ZERO, "0").send();
        return TransactionAPI.isStatusOK(receipt.getStatus());
    }

    private boolean deposit(CustomRawTransactionManager manager, BigInteger gasPrice, BigInteger gasLimit) throws Exception {
        ARPBank bank = ARPBank.load(manager, gasPrice, gasLimit);
        TransactionReceipt receipt = bank.deposit(new BigInteger(Convert.toWei(DEPOSIT_ARP_NUMBER, Convert.Unit.ETHER).toString())).send();
        return TransactionAPI.isStatusOK(receipt.getStatus());
    }

    private Boolean cash(Promise promise, String address, CustomRawTransactionManager manager, BigInteger gasPrice, BigInteger gasLimit) throws Exception {
        ARPBank bank = ARPBank.load(manager, gasPrice, gasLimit);
        TransactionReceipt receipt = bank.cash(promise, address).send();
        return TransactionAPI.isStatusOK(receipt.getStatus());
    }

    private Boolean withdraw(BigInteger amount, CustomRawTransactionManager manager, BigInteger gasPrice, BigInteger gasLimit) throws Exception {
        ARPBank bank = ARPBank.load(manager, gasPrice, gasLimit);
        TransactionReceipt receipt = bank.withdraw(amount).send();
        return TransactionAPI.isStatusOK(receipt.getStatus());
    }

    private class HashBackListener implements CustomRawTransactionManager.OnHashBackListener {
        private String txHash;
        private int opType;

        private BigInteger amount;
        private String args;

        public HashBackListener(int opType) {
            this(opType, "");
        }

        public HashBackListener(int opType, String args) {
            this.opType = opType;
            this.args = args;
        }

        public HashBackListener(int opType, BigInteger amount) {
            this.opType = opType;
            this.amount = amount;
        }

        @Override
        public void onHash(String transactionHash) {
            Log.d(TAG, "HashBackListener onHash = " + transactionHash);
            txHash = transactionHash;
            saveHashToDb(transactionHash, opType, args);
            if (opType == OPERATION_CASH) {
                savePendingToDb(transactionHash, amount);
            }
        }

        private String getTxHash() {
            return txHash;
        }
    }
}
