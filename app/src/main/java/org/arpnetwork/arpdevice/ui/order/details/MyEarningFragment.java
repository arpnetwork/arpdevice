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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.config.Constant;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.contracts.tasks.SimpleOnValueResult;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.database.EarningRecord;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.miner.StateHolder;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.util.UIHelper;
import org.arpnetwork.arpdevice.util.Util;
import org.spongycastle.util.encoders.Hex;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.arpnetwork.arpdevice.config.Constant.KEY_EXCHANGE_AMOUNT;
import static org.arpnetwork.arpdevice.config.Constant.KEY_EXCHANGE_TYPE;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerIntentService.OPERATION_CASH;

public class MyEarningFragment extends BaseFragment {
    private static final String TAG = MyEarningFragment.class.getSimpleName();

    private MyEarningAdapter mAdapter;
    private MyEarningHeader mHeaderView;

    private boolean mLoading;
    private BigInteger exchanged = BigInteger.ZERO;
    private BigInteger mUnexchanged = BigInteger.ZERO;
    private BigInteger mTopCid = BigInteger.ZERO;
    private BigInteger mTopAmount = BigInteger.ZERO;

    private BindStateReceiver mBindStateReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.my_earnings);

        registerReceiver();
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
        if (mBindStateReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBindStateReceiver);
            mBindStateReceiver = null;
        }
    }

    private void registerReceiver() {
        IntentFilter statusIntentFilter = new IntentFilter(
                Constant.BROADCAST_ACTION_STATUS);
        statusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        mBindStateReceiver = new BindStateReceiver();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mBindStateReceiver,
                statusIntentFilter);
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
                    Bundle bundle = new Bundle();
                    bundle.putInt(KEY_EXCHANGE_TYPE, OPERATION_CASH);
                    bundle.putString(KEY_EXCHANGE_AMOUNT, mUnexchanged.toString());
                    startActivity(ExchangeActivity.class, bundle);
                }
            }
        });

        mAdapter = new MyEarningAdapter(getContext());
        ListView listView = (ListView) findViewById(R.id.listview);
        listView.addHeaderView(mHeaderView);
        listView.setAdapter(mAdapter);
    }

    private void refreshData() {
        loadNextRemote();
        List<EarningRecord> oneTime = EarningRecord.findAll();
        mAdapter.setData(oneTime);
        mHeaderView.setData(Util.getHumanicAmount(exchanged), mAdapter.getCount() > 0);
        getUnexchange();
    }

    @Override
    protected void loadData() {
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

        mHeaderView.setData(Util.getHumanicAmount(exchanged), mAdapter.getCount() > 0);
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

        EarningRecord earning = EarningRecord.get(log.getTransactionHash());
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
        ARPBank.getUnexchangeAsync(new SimpleOnValueResult<BigInteger>() {
            @Override
            public void onValueResult(BigInteger result) {
                mUnexchanged = result;
                mHeaderView.setUnexchanged(Convert.fromWei(new BigDecimal(mUnexchanged), Convert.Unit.ETHER).floatValue());
            }

            @Override
            public void onFail(Throwable throwable) {
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
        });
    }

    private void setTopAmount(BigInteger cid, BigInteger amount) {
        if (mTopCid.compareTo(BigInteger.ZERO) == 0 || mTopCid.compareTo(cid) != 0) {
            mTopCid = cid;
            mTopAmount = amount;
        } else {
            mTopAmount = mTopAmount.add(amount);
        }
    }

    private class BindStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(Constant.EXTENDED_DATA_STATUS, StateHolder.STATE_BANK_CASH_RUNNING)) {
                case StateHolder.STATE_BANK_CASH_RUNNING:
                    refreshData();
                    break;

                case StateHolder.STATE_BANK_CASH_SUCCESS:
                    UIHelper.showToast(getActivity(), getString(R.string.exchange_success));
                    refreshData();
                    break;

                case StateHolder.STATE_BANK_CASH_FAILED:
                    UIHelper.showToast(getActivity(), getString(R.string.exchange_failed));
                    refreshData();
                    break;

                default:
                    break;
            }
        }
    }
}
