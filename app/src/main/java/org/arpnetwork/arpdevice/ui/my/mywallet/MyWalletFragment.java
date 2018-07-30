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

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.contract.BalanceAPI;
import org.arpnetwork.arpdevice.contract.tasks.OnValueResult;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.ui.wallet.WalletImporterActivity;
import org.arpnetwork.arpdevice.ui.wallet.WalletManager;

import java.math.BigDecimal;

public class MyWalletFragment extends BaseFragment {

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
        Wallet myWallet = WalletManager.getInstance().getWallet();
        nameText.setText(myWallet.getName());

        final TextView arpBalanceText = (TextView) findViewById(R.id.tv_arp_balance);
        BalanceAPI.getArpBalance(myWallet.getPublicKey(), new OnValueResult() {
            @Override
            public void onValueResult(BigDecimal result) {
                arpBalanceText.setText(String.format("%.4f", result));
            }
        });

        final TextView ethBalanceText = (TextView) findViewById(R.id.tv_eth_balance);
        BalanceAPI.getEtherBalance(myWallet.getPublicKey(), new OnValueResult() {
            @Override
            public void onValueResult(BigDecimal result) {
                ethBalanceText.setText(String.format("%.4f", result));
            }
        });

        Button resetButton = (Button) findViewById(R.id.btn_reset);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetWallet();
            }
        });
    }

    private void resetWallet() {
        Intent intent = new Intent(getActivity(), WalletImporterActivity.class);
        startActivity(intent);
        finish();
    }
}
