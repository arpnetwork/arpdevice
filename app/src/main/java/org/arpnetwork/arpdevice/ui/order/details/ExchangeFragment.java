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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.constant.Constant;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.contracts.ARPContract;
import org.arpnetwork.arpdevice.contracts.tasks.SimpleOnValueResult;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.miner.BindMinerActivity;
import org.arpnetwork.arpdevice.ui.miner.BindMinerIntentService;
import org.arpnetwork.arpdevice.ui.miner.StateHolder;
import org.arpnetwork.arpdevice.ui.widget.GasFeeView;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.util.UIHelper;
import org.web3j.utils.Convert;

import java.math.BigInteger;

import static org.arpnetwork.arpdevice.constant.Constant.KEY_EXCHANGE_AMOUNT;
import static org.arpnetwork.arpdevice.constant.Constant.KEY_GASLIMIT;
import static org.arpnetwork.arpdevice.constant.Constant.KEY_GASPRICE;
import static org.arpnetwork.arpdevice.constant.Constant.KEY_OP;
import static org.arpnetwork.arpdevice.constant.Constant.KEY_PASSWD;
import static org.arpnetwork.arpdevice.contracts.ARPBank.DEPOSIT_ARP_NUMBER;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerIntentService.OPERATION_CASH;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerIntentService.OPERATION_WITHDRAW;
import static org.arpnetwork.arpdevice.util.Util.getHumanicAmount;

public class ExchangeFragment extends BaseFragment {
    private static BigInteger mGasLimit = BigInteger.ZERO;

    private LinearLayout mProgressView;
    private TextView mProgressTip;
    private TextView mTotalAmountText;
    private TextView mTotalAmountValue;
    private TextView mExchangeValue;
    private GasFeeView mGasView;
    private EditText mPasswordText;
    private Button mExchangeBtn;

    private int mType;
    private BigInteger mAmount;
    private Miner mMiner;

    private BindStateReceiver mBindStateReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mType = getArguments().getInt(Constant.KEY_EXCHANGE_TYPE, OPERATION_CASH);
        String amountString = getArguments().getString(Constant.KEY_EXCHANGE_AMOUNT);
        if (amountString != null) {
            mAmount = new BigInteger(amountString);
        } else {
            mAmount = Convert.toWei(DEPOSIT_ARP_NUMBER, Convert.Unit.ETHER).toBigInteger();
        }
        mMiner = (Miner) getArguments().getSerializable(Constant.KEY_MINER);

        registerReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exchange, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        initViews();
        loadData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mGasView.cancelHttp();

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
        mProgressView = (LinearLayout) findViewById(R.id.ll_progress);
        mProgressTip = (TextView) findViewById(R.id.tv_progress_tip);
        mTotalAmountText = (TextView) findViewById(R.id.tv_amount_text);
        mTotalAmountValue = (TextView) findViewById(R.id.tv_amount);
        mExchangeValue = (TextView) findViewById(R.id.tv_exchange_amount);
        mGasView = (GasFeeView) findViewById(R.id.ll_gas_fee);
        mPasswordText = (EditText) findViewById(R.id.et_password);
        mExchangeBtn = (Button) findViewById(R.id.btn_exchange);

        mExchangeValue.setText(String.format(getString(R.string.exchange_arp_amount), getHumanicAmount(mAmount)));

        setState();

        mGasView.setEthCallback(new GasFeeView.EthCallback() {
            @Override
            public void onEthNotEnough(boolean notEnough, BigInteger ethBalance) {
                if (notEnough) {
                    showErrorAlertDialog(R.string.register_underpaid, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void loadData() {
        ARPContract.balanceOfAsync(Wallet.get().getAddress(), new SimpleOnValueResult<BigInteger>() {
            @Override
            public void onValueResult(BigInteger result) {
                mTotalAmountValue.setText(String.format(getString(R.string.exchange_total_amount),
                        getHumanicAmount(result)));
            }

            @Override
            public void onFail(Throwable throwable) {
                mTotalAmountValue.setText("load failed");
            }
        });
    }

    private void setState() {
        switch (mType) {
            case OPERATION_CASH:
                setTitle(R.string.exchange_arp);
                mTotalAmountText.setText(R.string.exchange_arp);
                mExchangeBtn.setText(R.string.exchange);

                mGasLimit = ARPBank.estimateCashGasLimit(Promise.get(), Wallet.get().getAddress());
                mGasView.setGasLimit(mGasLimit);

                mExchangeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showProgress(R.string.handling);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (isCorrectPassword()) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            hideSoftInput(mExchangeBtn);

                                            startServiceIntent(OPERATION_CASH);
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            hideProgress();

                                            UIHelper.showToast(getActivity(), getString(R.string.input_passwd_error));
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                });
                break;

            case OPERATION_WITHDRAW:
                setTitle(R.string.withdraw);
                mTotalAmountText.setText(R.string.withdraw);
                mExchangeBtn.setText(R.string.withdraw);

                mGasLimit = ARPBank.estimateWithdrawGasLimit(mAmount);
                mGasView.setGasLimit(mGasLimit);

                mExchangeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showProgress(R.string.handling);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (isCorrectPassword()) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            hideSoftInput(mExchangeBtn);

                                            startServiceIntent(OPERATION_WITHDRAW);
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            hideProgress();

                                            UIHelper.showToast(getActivity(), getString(R.string.input_passwd_error));
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                });
                break;
        }
    }

    private void showProgress(int textId) {
        mProgressTip.setText(textId);
        mProgressView.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mProgressView.setVisibility(View.GONE);
    }

    private boolean isCorrectPassword() {
        String password = mPasswordText.getText().toString();
        return !TextUtils.isEmpty(password) && Wallet.loadWalletCredentials(password) != null;
    }

    private void startServiceIntent(int opType) {
        Intent serviceIntent = new Intent(getActivity(), BindMinerIntentService.class);
        serviceIntent.putExtra(KEY_OP, opType);
        serviceIntent.putExtra(KEY_EXCHANGE_AMOUNT, mAmount.toString());
        serviceIntent.putExtra(KEY_PASSWD, mPasswordText.getText().toString());
        serviceIntent.putExtra(KEY_GASPRICE, mGasView.getGasPrice().toString());
        serviceIntent.putExtra(KEY_GASLIMIT, mGasLimit.toString());
        getActivity().startService(serviceIntent);
    }

    private class BindStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(Constant.EXTENDED_DATA_STATUS, StateHolder.STATE_BANK_CASH_RUNNING)) {
                case StateHolder.STATE_BANK_CASH_RUNNING:
                    showProgress(R.string.cashing);
                    break;

                case StateHolder.STATE_BANK_CASH_SUCCESS:
                    UIHelper.showToast(getActivity(), getString(R.string.exchange_success));
                    hideProgress();
                    if (mMiner != null) {
                        Bundle bundle = new Bundle();
                        bundle.putSerializable(Constant.KEY_MINER, mMiner);
                        startActivity(BindMinerActivity.class, bundle);
                    }
                    finish();
                    break;

                case StateHolder.STATE_BANK_CASH_FAILED:
                    UIHelper.showToast(getActivity(), getString(R.string.exchange_failed));
                    hideProgress();
                    break;

                case StateHolder.STATE_BANK_WITHDRAW_RUNNING:
                    showProgress(R.string.withdrawing);
                    break;

                case StateHolder.STATE_BANK_WITHDRAW_SUCCESS:
                    UIHelper.showToast(getActivity(), getString(R.string.withdraw_success));
                    hideProgress();
                    finish();
                    break;

                case StateHolder.STATE_BANK_WITHDRAW_FAILED:
                    UIHelper.showToast(getActivity(), getString(R.string.withdraw_failed));
                    hideProgress();
                    break;

                default:
                    break;
            }
        }
    }
}
