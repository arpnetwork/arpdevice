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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.contracts.ARPContract;
import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.contracts.tasks.OnValueResult;
import org.arpnetwork.arpdevice.dialog.PayEthDialog;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.miner.BindMinerHelper;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.ui.wallet.WalletImporterActivity;
import org.web3j.crypto.Credentials;

import java.math.BigDecimal;
import java.math.BigInteger;

public class MyWalletFragment extends BaseFragment {
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

        final TextView arpBalanceText = (TextView) findViewById(R.id.tv_arp_balance);
        ARPContract.getArpBalance(myWallet.getAddress(), new OnValueResult<BigDecimal>() {
            @Override
            public void onValueResult(BigDecimal result) {
                setBalance(result, arpBalanceText);
            }
        });

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
                resetWallet();
            }
        });

        mWithdrawBtn = (Button) findViewById(R.id.btn_withdraw);
        mWithdrawBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PayEthDialog.showPayEthDialog(getActivity(), new PayEthDialog.OnPayListener() {
                    @Override
                    public void onPay(BigInteger priceWei, BigInteger gasUsed, String password) {
                        withdraw(Wallet.loadCredentials(password), priceWei);
                    }
                });
            }
        });
        final String deviceAddress = Wallet.get().getAddress();
        ARPBank.balanceOf(deviceAddress, new OnValueResult<BigDecimal>() {
            @Override
            public void onValueResult(BigDecimal result) {
                if (BindMinerHelper.getBound(deviceAddress) == null && result != null && result.doubleValue() > 0) {
                    mWithdrawBtn.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void resetWallet() {
        Intent intent = new Intent(getActivity(), WalletImporterActivity.class);
        startActivity(intent);
        finish();
    }

    private void withdraw(Credentials credentials, BigInteger gasPrice) {
        showProgressDialog(getString(R.string.handling));
        ARPBank.withdrawAll(credentials, gasPrice, new OnValueResult<Boolean>() {
            @Override
            public void onValueResult(Boolean result) {
                if (getActivity() == null) return;
                hideProgressDialog();
                if (result) {
                    mWithdrawBtn.setVisibility(View.GONE);
                    Toast.makeText(getContext(), getString(R.string.withdraw_success), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), getString(R.string.withdraw_failed), Toast.LENGTH_SHORT).show();
                }
            }
        });
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
