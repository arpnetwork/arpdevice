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

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.contracts.tasks.OnValueResult;
import org.arpnetwork.arpdevice.contracts.tasks.TransactionTask2;
import org.arpnetwork.arpdevice.data.BankAllowance;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.dialog.PayEthDialog;
import org.arpnetwork.arpdevice.database.EarningRecord;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.miner.BindMinerHelper;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.util.UIHelper;
import org.spongycastle.util.encoders.Hex;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MyEarningFragment extends BaseFragment {
    private MyEarningAdapter mAdapter;
    private MyEarningHeader mHeaderView;

    private boolean mLoading;
    private boolean mFirstLoad = true;
    private float exchanged;
    private BigInteger mUnexchanged;

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
        View footerView = new View(getContext());
        footerView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.content_padding)));
        footerView.setBackgroundResource(R.color.window_background_light_gray);

        mHeaderView = new MyEarningHeader(getContext(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Promise promise = Promise.get();
                if (promise == null || mUnexchanged.compareTo(BigInteger.ZERO) == 0) {
                    UIHelper.showToast(getContext(), getString(R.string.exchange_tip_no_promise), Toast.LENGTH_SHORT);
                } else if (EarningRecord.find(EarningRecord.STATE_PENDING) != null) {
                    UIHelper.showToast(CustomApplication.sInstance,
                            getString(R.string.exchange_tip_exchanging), Toast.LENGTH_SHORT);
                } else {
                    final String spender = Wallet.get().getAddress();
                    PayEthDialog.showPayEthDialog(getActivity(), new PayEthDialog.OnPayListener() {
                        @Override
                        public void onPay(BigInteger priceWei, BigInteger gasUsed, String password) {
                            ARPBank.cash(promise, spender, Wallet.loadCredentials(password), priceWei, new TransactionTask2.OnTransactionCallback<Boolean>() {
                                @Override
                                public void onTxHash(String txHash) {
                                    final EarningRecord localRecord = savePendingToDb(txHash);
                                    mHeaderView.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            List<EarningRecord> local = new ArrayList<>(1);
                                            local.add(localRecord);
                                            mAdapter.addData(local);
                                        }
                                    });
                                }

                                @Override
                                public void onValueResult(Boolean result) {
                                    if (result) {
                                        if (mHeaderView != null) {
                                            refreshData();
                                        } else {
                                            UIHelper.showToast(CustomApplication.sInstance,
                                                    getString(R.string.exchange_success), Toast.LENGTH_SHORT);
                                        }
                                    } else {
                                        UIHelper.showToast(CustomApplication.sInstance,
                                                getString(R.string.exchange_failed), Toast.LENGTH_SHORT);
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });

        mAdapter = new MyEarningAdapter(getContext());
        ListView listView = (ListView) findViewById(R.id.listview);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            private boolean mToBottom;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE && mToBottom) {
                    loadData();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                mToBottom = false;
                if (totalItemCount > 0 && firstVisibleItem + visibleItemCount == totalItemCount) {
                    mToBottom = true;
                }
            }
        });
        listView.addHeaderView(mHeaderView);
        listView.addFooterView(footerView);
        listView.setAdapter(mAdapter);
    }

    private EarningRecord savePendingToDb(String txHash) {
        final EarningRecord localRecord = new EarningRecord();
        localRecord.state = EarningRecord.STATE_PENDING;
        localRecord.time = System.currentTimeMillis();
        localRecord.earning = mUnexchanged.toString();
        localRecord.transactionHash = txHash;
        localRecord.minerAddress = Promise.get().getFrom();
        localRecord.saveRecord();

        return localRecord;
    }

    private void refreshData() {
        loadNextRemote();
        List<EarningRecord> oneTime = EarningRecord.findAll();
        for (EarningRecord record : oneTime) {
            if (record.state == EarningRecord.STATE_SUCCESS) {
                exchanged += record.getEarning();
            }
        }
        mAdapter.setData(oneTime);
        mHeaderView.setData(exchanged, mAdapter.getCount() > 0);
        getUnexchange();
    }

    private void loadData() {
        if (mLoading) {
            return;
        }

        List<EarningRecord> oneTime;
        if (mFirstLoad) {
            mFirstLoad = false;
            if (EarningRecord.findTop() != null) {
                oneTime = EarningRecord.findAll();
                for (EarningRecord record : oneTime) {
                    if (record.state == EarningRecord.STATE_SUCCESS) {
                        exchanged += record.getEarning();
                    }
                }
                if (EarningRecord.find(EarningRecord.STATE_PENDING) != null) {
                    loadNextRemote(); // Update state.
                }
            } else {
                oneTime = loadNextRemote();
            }
        } else {
            oneTime = loadNextRemote();
        }

        mLoading = true;

        mAdapter.addData(oneTime);
        mHeaderView.setData(exchanged, mAdapter.getCount() > 0);
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
        } catch (ExecutionException e) {
        } catch (InterruptedException e) {
        }

        if (transactionList != null && transactionList.size() > 0) {
            for (Log log : transactionList) {
                EarningRecord newEarning = getEarningByLog(log);
                exchanged += newEarning.getEarning();
                records.add(newEarning);
            }
        }
        return records;
    }

    private EarningRecord getEarningByLog(Log log) {
        long logDate = 0;
        try {
            logDate = EtherAPI.getTransferDate(log.getBlockHash());
        } catch (ExecutionException ignore) {
        } catch (InterruptedException ignore) {
        }

        byte[] data = Hex.decode(Numeric.cleanHexPrefix(log.getData()));
        byte[] amountByte = new byte[32];
        System.arraycopy(data, 32, amountByte, 0, 32);
        BigInteger amount = new BigInteger(amountByte);

        byte[] topic = Hex.decode(Numeric.cleanHexPrefix(log.getTopics().get(1)));
        byte[] addressByte = new byte[20];
        System.arraycopy(topic, 12, addressByte, 0, 20);

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
        if (Promise.get() == null) return;

        final BigInteger amount = new BigInteger(Promise.get().getAmount(), 16);
        String address = Wallet.get().getAddress();
        Miner miner = BindMinerHelper.getBound(address);
        if (miner != null) {
            BankAllowance allowance = ARPBank.allowanceARP(miner.getAddress(), Wallet.get().getAddress());
            mUnexchanged = amount.subtract(allowance.paid);
            float show = Convert.fromWei(new BigDecimal(mUnexchanged), Convert.Unit.ETHER).floatValue();
            mHeaderView.setUnexchanged(show);
        }
    }
}
