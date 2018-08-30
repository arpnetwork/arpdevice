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

package org.arpnetwork.arpdevice.ui.my.mywallet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.contracts.ARPContract;
import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.contracts.api.TransactionAPI;
import org.arpnetwork.arpdevice.contracts.tasks.OnValueResult;
import org.arpnetwork.arpdevice.dialog.MessageDialog;
import org.arpnetwork.arpdevice.dialog.PayEthDialog;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.miner.BindMinerHelper;
import org.arpnetwork.arpdevice.ui.order.details.MyEarningActivity;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.ui.wallet.WalletImporterActivity;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;

public class MyWalletFragment extends BaseFragment {
    private static final String TAG = MyWalletFragment.class.getSimpleName();
    private boolean alertShown = false;

    private Button mWithdrawBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.my_wallet);
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

    private void initViews() {
        TextView nameText = (TextView) findViewById(R.id.tv_name);
        Wallet myWallet = Wallet.get();
        nameText.setText(myWallet.getName());

        TextView addrText = (TextView) findViewById(R.id.tv_addr);
        addrText.setText(myWallet.getAddress());

        getARPBalance(myWallet.getAddress());

        final TextView ethBalanceText = (TextView) findViewById(R.id.tv_eth_balance);
        EtherAPI.getEtherBalance(myWallet.getAddress(), new OnValueResult<BigDecimal>() {
            @Override
            public void onValueResult(BigDecimal result) {
                setBalance(result, ethBalanceText);
            }
        });

        Button resetButton = (Button) findViewById(R.id.btn_reset);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getUnexchange();
            }
        });

        mWithdrawBtn = (Button) findViewById(R.id.btn_withdraw);
        mWithdrawBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final BigInteger amount = ARPBank.balanceOf(Wallet.get().getAddress());
                if (amount.compareTo(BigInteger.ZERO) == 0) {
                    Toast.makeText(getContext(), R.string.network_error, Toast.LENGTH_SHORT).show();
                } else {
                    final BigInteger gasLimit = ARPBank.estimateWithdrawGasLimit(amount);
                    PayEthDialog.showPayEthDialog(getActivity(), gasLimit, new PayEthDialog.OnPayListener() {
                        @Override
                        public void onPay(BigInteger priceWei, String password) {
                            withdraw(Wallet.loadCredentials(password), amount, priceWei, gasLimit);
                        }
                    });
                }
            }
        });
        final String deviceAddress = Wallet.get().getAddress();
        BigInteger balance = ARPBank.balanceOf(deviceAddress);
        float floatValue = balance.floatValue();
        if (BindMinerHelper.getBound(deviceAddress) == null && balance != null && floatValue > 0) {
            mWithdrawBtn.setVisibility(View.VISIBLE);
        }
    }

    private void resetWallet() {
        Intent intent = new Intent(getActivity(), WalletImporterActivity.class);
        startActivity(intent);
        finish();
    }

    private void getUnexchange() {
        BigInteger unexchanged = BigInteger.ZERO;
        try {
            unexchanged = ARPBank.getUnexchange();
        } catch (Exception e) {
            Log.d(TAG, "getUnexchange: error:" + e.getMessage());
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
            builder.setTitle(getString(R.string.exchange_change_miner_title))
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.exchange), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent();
                            intent.setClass(getActivity(), MyEarningActivity.class);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton(getString(R.string.exchange_change_miner_cancel), new DialogInterface.OnClickListener() {
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

    private void withdraw(final Credentials credentials, final BigInteger amount, final BigInteger gasPrice, final BigInteger gasLimit) {
        showProgressDialog(getString(R.string.handling));

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ARPBank bank = ARPBank.load(credentials, gasPrice, gasLimit);
                TransactionReceipt receipt = null;
                try {
                    receipt = bank.withdraw(amount).send();
                } catch (Exception e) {
                    Log.e(TAG, "withdraw error:" + e.getCause());
                    return;
                }

                boolean success = receipt != null && TransactionAPI.isStatusOK(receipt.getStatus());
                hideProgressDialog();
                if (success) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (getActivity() != null && !getActivity().isDestroyed()) {
                                mWithdrawBtn.setVisibility(View.GONE);
                                getARPBalance(Wallet.get().getAddress());
                            }
                            Toast.makeText(CustomApplication.sInstance,
                                    CustomApplication.sInstance.getString(R.string.withdraw_success),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(CustomApplication.sInstance,
                                    CustomApplication.sInstance.getString(R.string.withdraw_failed),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        };
        new Thread(runnable).start();
    }

    private void getARPBalance(String address) {
        TextView arpBalanceText = (TextView) findViewById(R.id.tv_arp_balance);
        BigInteger balance = ARPContract.balanceOf(address);
        setBalance(Convert.fromWei(balance.toString(), Convert.Unit.ETHER), arpBalanceText);
    }

    private void setBalance(BigDecimal balance, TextView textView) {
        if (balance == null) {
            showErrorAlertDialog();
        } else {
            textView.setText(String.format("%.4f", balance));
        }
    }

    private void showErrorAlertDialog() {
        if (!alertShown && getContext() != null) {
            alertShown = true;
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.get_balance_error_msg)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }
}
