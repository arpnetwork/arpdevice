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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.contracts.tasks.OnValueResult;
import org.arpnetwork.arpdevice.data.BankAllowance;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.dialog.PayEthDialog;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.miner.BindMinerHelper;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.spongycastle.util.encoders.Hex;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MyEarningFragment extends BaseFragment {
    private MyEarningAdapter mAdapter;
    private MyEarningHeader mHeaderView;
    private boolean mLoading;

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
                if (promise == null) {
                    Toast.makeText(getContext(), getString(R.string.exchange_tip_no_promise),Toast.LENGTH_SHORT)
                            .show();
//                } else if () { // Fixme: handle exchanging
                } else {
                    PayEthDialog.showPayEthDialog(getActivity(), new PayEthDialog.OnPayListener() {
                        @Override
                        public void onPay(BigInteger priceWei, BigInteger gasUsed, String password) {
                            ARPBank.cash(promise, Wallet.loadCredentials(password), priceWei, gasUsed, new OnValueResult<Boolean>() {
                                @Override
                                public void onValueResult(Boolean result) {
                                    if (result) {
                                        if (mHeaderView != null) {
                                            loadData();
                                        }
                                    } else {
                                        Toast.makeText(CustomApplication.sInstance,
                                                getString(R.string.exchange_failed), Toast.LENGTH_SHORT)
                                                .show();
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

    private void loadData() {
        if (mLoading) {
            return;
        }

        EarningData earningData = new EarningData();
        // FIXME：set history earning
        earningData.exchanged = 0;
        earningData.unexchanged = 0;

        mLoading = true;

        // FIXME: set record
        List<Earning> earningList = new ArrayList<>();
        List<Log> transactionList = null;
        try {
            // FIXME: check logs from the latest block of history record
            transactionList = ARPBank.getTransactionList(Wallet.get().getAddress(), new BigInteger("0"));
        } catch (ExecutionException e) {
        } catch (InterruptedException e) {
        }

        if (transactionList != null && transactionList.size() > 0) {
            for (Log log : transactionList) {
                Earning newEarning = getEarningByLog(log);
                earningList.add(0, newEarning);
                earningData.exchanged += newEarning.earning;
            }
        }

        earningData.earningList = earningList;
        mHeaderView.setData(earningData);
        mAdapter.setData(earningData.earningList);
        getUnexchange();
        mLoading = false;
    }

    private Earning getEarningByLog(Log log) {
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

        byte[] topic = Hex.decode(Numeric.cleanHexPrefix(log.getTopics().get(2)));
        byte[] addressByte = new byte[20];
        System.arraycopy(topic, 12, addressByte, 0, 20);

        Earning earning = new Earning();
        earning.time = logDate;
        earning.earning = Convert.fromWei(amount.toString(), Convert.Unit.ETHER).floatValue();
        earning.minerAddress = "0x" + Hex.toHexString(addressByte);
        return earning;
    }

    private void getUnexchange() {
        final BigInteger amount = new BigInteger(Promise.get().getAmount(), 16);
        String address = Wallet.get().getAddress();
        Miner miner = BindMinerHelper.getBound(address);

        if (miner != null) {
            ARPBank.allowanceARP(miner.getAddress(), Wallet.get().getAddress(), new OnValueResult<BankAllowance>() {
                @Override
                public void onValueResult(BankAllowance result) {
                    BigInteger unexchanged = amount.subtract(result.paid);
                    float show = Convert.fromWei(new BigDecimal(unexchanged), Convert.Unit.ETHER).floatValue();
                    mHeaderView.setUnexchanged(show);
                }
            });
        }
    }
}
