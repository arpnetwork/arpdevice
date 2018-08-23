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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.dialog.PayEthDialog;
import org.arpnetwork.arpdevice.util.Util;

import java.math.BigInteger;

public class ExchangeDialogActivity extends Activity {
    private static final String KEY_ARGS = "key_args";

    public static void launch(Context activity, String args) {
        Intent intent = new Intent();
        intent.setClass(activity, ExchangeDialogActivity.class);
        intent.putExtra(KEY_ARGS, args);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exchange_dialog);

        initViews();
    }

    private void initViews() {
        TextView title = ((TextView) findViewById(R.id.tv_title));
        title.setText(R.string.exchange_tip_title);
        TextView message = ((TextView) findViewById(R.id.tv_message));

        String args = getIntent().getExtras().getString(KEY_ARGS);
        BigInteger unExchanged = new BigInteger(args.split("#")[1]);
        long minerExp = Long.valueOf(args.split("#")[0]);

        String minerExpDate = Util.getDateTime("yyyy-MM-dd HH:mm", minerExp);
        String msg = String.format(getString(R.string.exchange_tip_msg), unExchanged, minerExpDate);
        message.setText(msg);

        Button cancel = findViewById(R.id.btn_cancel);
        cancel.setText(getString(R.string.exchange_tip_cancel));
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button ok = findViewById(R.id.btn_ok);
        ok.setText(getString(R.string.exchange));
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PayEthDialog.showPayEthDialog(ExchangeDialogActivity.this, getString(R.string.exchange), new PayEthDialog.OnPayListener() {
                    @Override
                    public void onPay(BigInteger priceWei, BigInteger gasUsed, String password) {
                        // TODO: add pay.
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
            }
        });

    }
}
