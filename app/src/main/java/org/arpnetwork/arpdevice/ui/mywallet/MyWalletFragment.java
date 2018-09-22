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

package org.arpnetwork.arpdevice.ui.mywallet;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.config.Constant;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.contracts.ARPContract;
import org.arpnetwork.arpdevice.contracts.ARPRegistry;
import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.contracts.tasks.SimpleOnValueResult;
import org.arpnetwork.arpdevice.data.BankAllowance;
import org.arpnetwork.arpdevice.dialog.MessageDialog;
import org.arpnetwork.arpdevice.dialog.PasswordDialog;
import org.arpnetwork.arpdevice.dialog.PromiseDialog;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.miner.BindMinerHelper;
import org.arpnetwork.arpdevice.ui.miner.StateHolder;
import org.arpnetwork.arpdevice.ui.order.details.ExchangeActivity;
import org.arpnetwork.arpdevice.ui.order.details.MyEarningActivity;
import org.arpnetwork.arpdevice.ui.unlock.UnlockActivity;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.ui.wallet.WalletImporterActivity;
import org.arpnetwork.arpdevice.util.UIHelper;
import org.arpnetwork.arpdevice.util.Util;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import static org.arpnetwork.arpdevice.config.Constant.KEY_EXCHANGE_AMOUNT;
import static org.arpnetwork.arpdevice.config.Constant.KEY_EXCHANGE_TYPE;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerIntentService.OPERATION_CASH;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerIntentService.OPERATION_WITHDRAW;
import static org.arpnetwork.arpdevice.util.Util.getHumanicAmount;

public class MyWalletFragment extends BaseFragment {
    private static final String TAG = MyWalletFragment.class.getSimpleName();

    private BigInteger mTotalAmount;
    private BigInteger mDepositAmount;

    private BindStateReceiver mBindStateReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.my_wallet);
        registerReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_wallet, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews();
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
        String address = Wallet.get().getAddress();

        TextView addrText = (TextView) findViewById(R.id.tv_addr);
        addrText.setText(address);

        getARPBalance(address);

        final TextView ethBalanceText = (TextView) findViewById(R.id.tv_eth_balance);
        EtherAPI.getEtherBalance(address, new SimpleOnValueResult<BigInteger>() {
            @Override
            public void onValueResult(BigInteger result) {
                if (getActivity() == null) return;

                ethBalanceText.setText(String.format("%.4f", Convert.fromWei(new BigDecimal(result), Convert.Unit.ETHER)));
            }

            @Override
            public void onFail(Throwable throwable) {
                showErrorAlertDialog(R.string.get_balance_error_msg);
            }
        });

        refreshDepositAmount();

        LinearLayout depositLayout = (LinearLayout) findViewById(R.id.ll_deposit);
        depositLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTotalAmount.compareTo(BigInteger.ZERO) > 0) {
                    if (!checkReady()) return;
                    checkBind(mTotalAmount);
                } else {
                    UIHelper.showToast(getContext(), R.string.no_deposit);
                }
            }
        });

        Button resetButton = (Button) findViewById(R.id.btn_reset);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkReady()) return;
                checkPromise();
            }
        });
    }

    private void refreshDepositAmount() {
        final String address = Wallet.get().getAddress();
        final TextView depositBalance = (TextView) findViewById(R.id.tv_deposit);

        new Thread(new Runnable() {
            @Override
            public void run() {
                BankAllowance allowance = null;
                try {
                    allowance = ARPBank.allowance(address, ARPRegistry.CONTRACT_ADDRESS);
                    if (allowance == null) return;
                    BigInteger allowanceAmount = allowance.amount;
                    mDepositAmount = ARPBank.balanceOf(address);
                    mTotalAmount = allowanceAmount.add(mDepositAmount);
                    depositBalance.post(new Runnable() {
                        @Override
                        public void run() {
                            depositBalance.setText(String.format(getString(R.string.float_arp_token), Util.getHumanicAmount(mTotalAmount)));
                        }
                    });
                } catch (IOException e) {
                    depositBalance.post(new Runnable() {
                        @Override
                        public void run() {
                            showErrorAlertDialog(R.string.get_balance_error_msg);
                        }
                    });
                }
            }
        }).start();
    }

    private void checkPromise() {
        PromiseDialog.show(getContext(), R.string.exchange_unbind_miner_msg,
                getString(R.string.exchange_unbind_miner_ignore),
                new PromiseDialog.PromiseListener() {
                    @Override
                    public void onError() {
                    }

                    @Override
                    public void onExchange(BigInteger unexchanged) {
                        Bundle bundle = new Bundle();
                        bundle.putInt(KEY_EXCHANGE_TYPE, OPERATION_CASH);
                        bundle.putString(KEY_EXCHANGE_AMOUNT, unexchanged.toString());
                        startActivity(ExchangeActivity.class, bundle);
                    }

                    @Override
                    public void onIgnore() {
                        checkAuthor();
                    }
                });
    }

    private void checkAuthor() {
        final PasswordDialog.Builder builder = new PasswordDialog.Builder(getContext());
        builder.setOnClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == PasswordDialog.CONFIRM) {
                    final String password = builder.getPassword();
                    if (TextUtils.isEmpty(password)) {
                        UIHelper.showToast(getActivity(), getString(R.string.input_passwd_tip));
                    } else {
                        dialog.dismiss();
                        if (Wallet.loadCredentials(password) != null) {
                            getUnexchange();
                        } else {
                            Toast.makeText(getContext(), getString(R.string.input_passwd_error), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });
        builder.create().show();
    }

    private void checkBind(final BigInteger totalAmount) {
        BindMinerHelper.getBoundAsync(Wallet.get().getAddress(), new SimpleOnValueResult<Miner>() {
            @Override
            public void onPreExecute() {
                showProgressDialog("", false);
            }

            @Override
            public void onValueResult(Miner result) {
                hideProgressDialog();

                if (result != null || mDepositAmount.compareTo(BigInteger.ZERO) == 0) {
                    final MessageDialog.Builder builder = new MessageDialog.Builder(getContext());
                    builder.setTitle(getString(R.string.withdraw_title));
                    if (result == null) {
                        builder.setMessage(getString(R.string.withdraw_tip_lock))
                                .setPositiveButton(getString(R.string.unlock), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        startActivity(UnlockActivity.class);
                                    }
                                })
                                .setNegativeButton(getString(R.string.cancel), null);
                    } else {
                        builder.setMessage(getString(R.string.withdraw_tip_unbind))
                                .setPositiveButton(getString(R.string.ok), null);
                    }
                    builder.create().show();
                } else {
                    showWithdraw(totalAmount);
                }
            }

            @Override
            public void onFail(Throwable throwable) {
                hideProgressDialog();
                UIHelper.showToast(getContext(), R.string.network_error);
            }
        });
    }

    private boolean checkReady() {
        if (StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING) != null) {
            UIHelper.showToast(getContext(), R.string.unbinding);
        } else if (StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING) != null) {
            UIHelper.showToast(getContext(), R.string.binding);
        } else if (StateHolder.getTaskByState(StateHolder.STATE_BANK_CASH_RUNNING) != null) {
            UIHelper.showToast(getContext(), R.string.cashing);
        } else if (StateHolder.getTaskByState(StateHolder.STATE_BANK_WITHDRAW_RUNNING) != null) {
            UIHelper.showToast(getContext(), R.string.withdrawing);
        } else {
            return true;
        }

        return false;
    }

    private void showWithdraw(BigInteger withdrawAmount) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_EXCHANGE_TYPE, OPERATION_WITHDRAW);
        bundle.putString(KEY_EXCHANGE_AMOUNT, withdrawAmount.toString());
        startActivity(ExchangeActivity.class, bundle);
    }

    private void getUnexchange() {
        BigInteger unexchanged = BigInteger.ZERO;
        try {
            unexchanged = ARPBank.getUnexchange();
        } catch (Exception e) {
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.tip_changing_wallet)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            resetWallet();
                        }
                    })
                    .setCancelable(true)
                    .show();
            return;
        }

        if (unexchanged.compareTo(BigInteger.ZERO) > 0) {
            String message = String.format(getString(R.string.unexchange_tip_changing_wallet),
                    Convert.fromWei(unexchanged.toString(), Convert.Unit.ETHER).floatValue());
            MessageDialog.Builder builder = new MessageDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.exchange_unchanged_promise_found))
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.go_exchange), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent();
                            intent.setClass(getActivity(), MyEarningActivity.class);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton(getString(R.string.exchange_change_wallet_ignore), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            resetWallet();
                        }
                    })
                    .create()
                    .show();
        } else {
            resetWallet();
        }
    }

    private void getARPBalance(String address) {
        final TextView arpBalanceText = (TextView) findViewById(R.id.tv_arp_balance);
        ARPContract.balanceOfAsync(address, new SimpleOnValueResult<BigInteger>() {
            @Override
            public void onValueResult(BigInteger result) {
                if (getActivity() == null) return;

                arpBalanceText.setText(String.format("%.4f", Convert.fromWei(result.toString(), Convert.Unit.ETHER)));
            }

            @Override
            public void onFail(Throwable throwable) {
                showErrorAlertDialog(R.string.get_balance_error_msg);
            }
        });
    }

    private void resetWallet() {
        Intent intent = new Intent(getActivity(), WalletImporterActivity.class);
        startActivity(intent);
        finish();
    }

    private class BindStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(Constant.EXTENDED_DATA_STATUS,
                    StateHolder.STATE_BANK_WITHDRAW_SUCCESS)) {
                case StateHolder.STATE_BANK_CASH_SUCCESS:
                    UIHelper.showToast(getActivity(), getString(R.string.exchange_success));
                    break;

                case StateHolder.STATE_BANK_CASH_FAILED:
                    UIHelper.showToast(getActivity(), getString(R.string.exchange_failed));
                    break;

                case StateHolder.STATE_BANK_WITHDRAW_SUCCESS:
                    UIHelper.showToast(getActivity(), getString(R.string.withdraw_success));
                    refreshDepositAmount();
                    getARPBalance(Wallet.get().getAddress());
                    break;

                case StateHolder.STATE_UNBIND_SUCCESS:
                    refreshDepositAmount();
                    break;

                case StateHolder.STATE_BANK_WITHDRAW_FAILED:
                    UIHelper.showToast(getActivity(), getString(R.string.withdraw_failed));
                    break;

                default:
                    break;
            }
        }
    }
}
