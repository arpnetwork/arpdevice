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

package org.arpnetwork.arpdevice.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.widget.SeekBar;

import org.arpnetwork.arpdevice.R;

import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.util.PriceProvider;
import org.arpnetwork.arpdevice.util.UIHelper;
import org.arpnetwork.arpdevice.util.Util;
import org.web3j.crypto.Credentials;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;

public class PayEthDialog {
    private static BigDecimal mGasPriceGWei;
    private static ProgressDialog mProgressDialog;
    private static Dialog mShowPriceDialog;
    private static DialogInterface.OnClickListener mCancelListener;

    public interface OnPayListener {
        void onPay(BigInteger priceWei, String password);
    }

    /**
     * Convenience method.
     */
    public static void showPayEthDialog(final Activity context, BigInteger gasLimit, OnPayListener callback) {
        showPayEthDialog(context, context.getString(R.string.pay_title), context.getString(R.string.pay_msg), null, gasLimit, callback, null);
    }

    public static void showPayEthDialog(final Activity context, final String title,
            final String message, final String positiveText, final BigInteger gasLimit,
            final OnPayListener callback, final DialogInterface.OnClickListener negativeListener) {
        if (mShowPriceDialog != null && mShowPriceDialog.isShowing()) return;

        mCancelListener = negativeListener;
        showProgressBar(context, "", false);
        PriceProvider.initOrLoadPrice(new PriceProvider.Callback() {
            @Override
            public void onPriceInfo(PriceProvider.PriceInfo priceInfo) {
                hideProgressBar();

                if (context == null || context.isDestroyed()) return;
                BigDecimal gasPriceGwei = new BigDecimal(priceInfo.getGasPriceGwei());
                final BigDecimal ethToYuanRateBig = new BigDecimal(priceInfo.getEthToYuanRate());

                final BigDecimal min = gasPriceGwei;
                final BigDecimal max = (gasPriceGwei.multiply(new BigDecimal("100")));
                BigDecimal defaultValue = gasPriceGwei;
                mGasPriceGWei = gasPriceGwei;

                final BigDecimal mEthSpend = Util.getEthCost(gasPriceGwei, gasLimit);
                double yuan = Util.getYuanCost(defaultValue, gasLimit, ethToYuanRateBig);

                final SeekBarDialog.Builder builder = new SeekBarDialog.Builder(context);
                builder.setTitle(title)
                        .setMessage(message)
                        .setSeekValue(0, String.format(context.getString(R.string.bind_eth_format), mEthSpend, yuan))
                        .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                if (fromUser) {
                                    // min + progress * (max - min) / 100;
                                    BigDecimal multiply = new BigDecimal(progress).multiply(max.subtract(min));
                                    BigDecimal divide = multiply.divide(new BigDecimal("100"));
                                    mGasPriceGWei = min.add(divide);
                                    BigDecimal mEthSpend = Util.getEthCost(mGasPriceGWei, gasLimit);
                                    double yuan = Util.getYuanCost(mGasPriceGWei, gasLimit, ethToYuanRateBig);
                                    builder.setSeekValue(progress, String.format(context.getString(R.string.bind_eth_format), mEthSpend, yuan));
                                }
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {
                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {
                            }
                        })
                        .setPositiveButton(TextUtils.isEmpty(positiveText) ? context.getString(R.string.ok) : positiveText, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showInputPasswdDialog(context, callback);
                            }
                        })
                        .setNegativeButton(context.getString(R.string.cancel), negativeListener);
                mShowPriceDialog = builder.create();
                mShowPriceDialog.show();
            }
        });
    }

    private static void showInputPasswdDialog(final Activity context, final OnPayListener callback) {
        final PasswordDialog.Builder builder = new PasswordDialog.Builder(context);
        builder.setOnClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                switch (which) {
                    case PasswordDialog.CANCEL:
                        mCancelListener.onClick(dialog, which);
                        break;

                    case PasswordDialog.CONFIRM:
                        final String password = builder.getPassword();
                        if (TextUtils.isEmpty(password)) {
                            UIHelper.showToast(context, context.getString(R.string.input_passwd_tip));
                        } else {
                            showProgressBar(context, "", false);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    final Credentials credentials = Wallet.loadCredentials(password);
                                    context.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            hideProgressBar();

                                            if (credentials == null) {
                                                UIHelper.showToast(context, context.getString(R.string.input_passwd_error));
                                            } else {
                                                dialog.dismiss();
                                                if (callback != null) {
                                                    callback.onPay(Convert.toWei(mGasPriceGWei, Convert.Unit.GWEI).toBigInteger(), password);
                                                }
                                            }
                                        }
                                    });
                                }
                            }).start();
                        }
                        break;
                }
            }
        });
        builder.create().show();
    }

    private static void showProgressBar(final Activity context, String msg, boolean cancel) {
        mProgressDialog = ProgressDialog.show(context, null, msg);
        mProgressDialog.setCanceledOnTouchOutside(cancel);
        mProgressDialog.setCancelable(cancel);
    }

    private static void hideProgressBar() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }
}
