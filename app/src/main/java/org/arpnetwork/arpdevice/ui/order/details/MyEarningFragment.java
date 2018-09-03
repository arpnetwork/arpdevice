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

package org.arpnetwork.arpdevice.ui.order.details;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.contracts.api.TransactionAPI;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.dialog.PayEthDialog;
import org.arpnetwork.arpdevice.database.EarningRecord;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.miner.StateHolder;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.util.UIHelper;
import org.spongycastle.util.encoders.Hex;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyEarningFragment extends BaseFragment {
    private static final String TAG = MyEarningFragment.class.getSimpleName();

    private MyEarningAdapter mAdapter;
    private MyEarningHeader mHeaderView;

    private boolean mLoading;
    private BigInteger exchanged = BigInteger.ZERO;
    private BigInteger mUnexchanged = BigInteger.ZERO;
    private BigInteger mTopCid = BigInteger.ZERO;
    private BigInteger mTopAmount = BigInteger.ZERO;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.my_earnings);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_earning, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews();
        loadData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initViews() {
        mHeaderView = new MyEarningHeader(getContext(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Promise promise = Promise.get();
                if (promise == null || mUnexchanged.compareTo(BigInteger.ZERO) == 0) {
                    UIHelper.showToast(getContext(), getString(R.string.exchange_tip_no_promise), Toast.LENGTH_SHORT);
                } else if (EarningRecord.find(EarningRecord.STATE_PENDING) != null) {
                    UIHelper.showToast(CustomApplication.sInstance,
                            getString(R.string.exchange_tip_exchanging), Toast.LENGTH_SHORT);
                } else if (StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING) != null) {
                    UIHelper.showToast(CustomApplication.sInstance, getString(R.string.unbinding_exchange), Toast.LENGTH_SHORT);
                } else {
                    final String spender = Wallet.get().getAddress();
                    final BigInteger gasLimit = ARPBank.estimateCashGasLimit(promise, spender);
                    PayEthDialog.showPayEthDialog(getActivity(), gasLimit, new PayEthDialog.OnPayListener() {
                        @Override
                        public void onPay(BigInteger priceWei, String password) {
                            payForExchange(password, priceWei, gasLimit, promise, spender);
                        }
                    });
                }
            }
        });

        mAdapter = new MyEarningAdapter(getContext());
        ListView listView = (ListView) findViewById(R.id.listview);
        listView.addHeaderView(mHeaderView);
        listView.setAdapter(mAdapter);
    }

    private void payForExchange(final String password, final BigInteger priceWei, final BigInteger gasLimit, final Promise promise, final String spender) {
        final EarningRecord localRecord = savePendingToDb(promise.getCid() + ":" + promise.getAmount());
        List<EarningRecord> local = new ArrayList<>(1);
        local.add(localRecord);
        mAdapter.addData(local);

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ARPBank bank = ARPBank.load(Wallet.loadCredentials(password), priceWei, gasLimit);
                TransactionReceipt receipt = null;
                try {
                    receipt = bank.cash(promise, spender).send();
                } catch (Exception e) {
                    android.util.Log.e(TAG, "onPay, cash error:" + e.getCause());
                    UIHelper.showToast(CustomApplication.sInstance,
                            getString(R.string.exchange_failed), Toast.LENGTH_SHORT);
                    return;
                }
                boolean success = TransactionAPI.isStatusOK(receipt.getStatus());
                Runnable callBack;
                if (success) {
                    callBack = new Runnable() {
                        @Override
                        public void run() {
                            if (getActivity() != null && !getActivity().isDestroyed()) {
                                refreshData();
                            }
                            UIHelper.showToast(CustomApplication.sInstance,
                                    CustomApplication.sInstance.getString(R.string.exchange_success), Toast.LENGTH_SHORT);
                        }
                    };
                } else {
                    callBack = new Runnable() {
                        @Override
                        public void run() {
                            UIHelper.showToast(CustomApplication.sInstance,
                                    CustomApplication.sInstance.getString(R.string.exchange_failed), Toast.LENGTH_SHORT);
                        }
                    };
                }
                handler.post(callBack);
            }
        };
        new Thread(runnable).start();
    }

    private EarningRecord savePendingToDb(String key) {
        final EarningRecord localRecord = new EarningRecord();
        localRecord.state = EarningRecord.STATE_PENDING;
        localRecord.time = System.currentTimeMillis();
        localRecord.earning = mUnexchanged.toString();
        localRecord.key = key;
        localRecord.minerAddress = Promise.get().getFrom();
        localRecord.saveRecord();

        return localRecord;
    }

    private void refreshData() {
        loadNextRemote();
        List<EarningRecord> oneTime = EarningRecord.findAll();
        mAdapter.setData(oneTime);
        mHeaderView.setData(getFloatExchanged(), mAdapter.getCount() > 0);
        getUnexchange();
    }

    private void loadData() {
        if (mLoading) {
            return;
        }

        mLoading = true;

        List<EarningRecord> oneTime;
        if (EarningRecord.findTop() != null) {
            exchanged = BigInteger.ZERO;
            oneTime = EarningRecord.findAll();
            Collections.reverse(oneTime); // Get latest record.
            for (EarningRecord record : oneTime) {
                if (record.state == EarningRecord.STATE_SUCCESS) {
                    exchanged = exchanged.add(record.getEarning());
                    setTopAmount(record.getCid(), record.getEarning());
                }
            }
            Collections.reverse(oneTime); // Displayed time decreased.
            if (EarningRecord.find(EarningRecord.STATE_PENDING) != null) {
                loadNextRemote(); // Update state.
            }
        } else {
            oneTime = loadNextRemote();
        }
        mAdapter.setData(oneTime);

        mHeaderView.setData(getFloatExchanged(), mAdapter.getCount() > 0);
        getUnexchange();
        mLoading = false;
    }

    private List<EarningRecord> loadNextRemote() {
        EarningRecord top = EarningRecord.findTopWithState(EarningRecord.STATE_SUCCESS);
        BigInteger topNextNumber = new BigInteger(Config.DEFAULT_BLOCKNUMBER);
        if (top != null && !TextUtils.isEmpty(top.blockNumber)) {
            topNextNumber = Numeric.toBigInt(top.blockNumber).add(BigInteger.ONE);
        }
        List<EarningRecord> result = getRemoteAndSave(topNextNumber);
        Collections.reverse(result);
        return result;
    }

    private List<EarningRecord> getRemoteAndSave(BigInteger blockNumber) {
        List<EarningRecord> records = new ArrayList<>();
        List<Log> transactionList = null;
        try {
            transactionList = ARPBank.getTransactionList(Wallet.get().getAddress(), blockNumber);
        } catch (Exception e) {
            android.util.Log.e(TAG, "getRemoteAndSave, getTransactionList error:" + e.getCause());
        }

        if (transactionList != null && transactionList.size() > 0) {
            for (Log log : transactionList) {
                EarningRecord newEarning = getEarningByLog(log);
                records.add(newEarning);
            }
        }
        return records;
    }

    private EarningRecord getEarningByLog(Log log) {
        long logDate = 0;
        try {
            logDate = EtherAPI.getTransferDate(log.getBlockHash());
        } catch (Exception e) {
            android.util.Log.e(TAG, "getEarningByLog, getTransferDate error:" + e.getCause());
        }

        byte[] data = Hex.decode(Numeric.cleanHexPrefix(log.getData()));
        byte[] cidByte = new byte[32];
        System.arraycopy(data, 0, cidByte, 0, 32);
        BigInteger cid = new BigInteger(cidByte);

        byte[] amountByte = new byte[32];
        System.arraycopy(data, 32, amountByte, 0, 32);
        BigInteger amount = new BigInteger(amountByte);

        byte[] topic = Hex.decode(Numeric.cleanHexPrefix(log.getTopics().get(1)));
        byte[] addressByte = new byte[20];
        System.arraycopy(topic, 12, addressByte, 0, 20);

        exchanged = exchanged.add(amount);
        setTopAmount(cid, amount);

        EarningRecord earning = EarningRecord.get(cid.toString(16) + ":" + mTopAmount.toString(16));
        earning.time = logDate;
        earning.earning = amount.toString();
        earning.minerAddress = "0x" + Hex.toHexString(addressByte);
        earning.blockNumber = log.getBlockNumberRaw();
        earning.state = EarningRecord.STATE_SUCCESS;
        earning.saveRecord();

        return earning;
    }

    private void getUnexchange() {
        mUnexchanged = BigInteger.ZERO;
        try {
            mUnexchanged = ARPBank.getUnexchange();
        } catch (Exception e) {
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.network_error)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(true)
                    .show();
        }
        mHeaderView.setUnexchanged(Convert.fromWei(new BigDecimal(mUnexchanged), Convert.Unit.ETHER).floatValue());
    }

    private void setTopAmount(BigInteger cid, BigInteger amount) {
        if (mTopCid.compareTo(BigInteger.ZERO) == 0 || mTopCid.compareTo(cid) != 0) {
            mTopCid = cid;
            mTopAmount = amount;
        } else {
            mTopAmount = mTopAmount.add(amount);
        }
    }

    private float getFloatExchanged() {
        return Convert.fromWei(exchanged.toString(), Convert.Unit.ETHER).floatValue();
    }
}
