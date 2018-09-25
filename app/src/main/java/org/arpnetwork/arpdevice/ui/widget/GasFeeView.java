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

package org.arpnetwork.arpdevice.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.util.PriceProvider;
import org.arpnetwork.arpdevice.util.Util;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;

public class GasFeeView extends LinearLayout {
    private static final String TAG = "GasFeeView";

    private static BigDecimal mGasPriceGWei = BigDecimal.ZERO;
    private static BigDecimal mEthToYuanRate = BigDecimal.ZERO;
    private static BigInteger mGasLimit = BigInteger.ZERO;
    private static BigInteger mGasCost = BigInteger.ZERO;

    private TextView mGasValue;
    private SeekBar mGasPriceBar;

    public GasFeeView(Context context) {
        super(context);
        init(context);
    }

    public GasFeeView(Context context, AttributeSet attr) {
        super(context, attr);
        init(context);
    }

    public GasFeeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.view_gas_fee, this);
        initView();
        loadGasPrice();
    }

    public void setGasLimit(BigInteger gasLimit) {
        mGasLimit = gasLimit;
        if (mGasPriceGWei.compareTo(BigDecimal.ZERO) > 0) {
            setGasValueText();
        }
    }

    public void cancelHttp() {
    }

    public BigInteger getGasCost() {
        return mGasCost;
    }

    public BigInteger getGasPrice() {
        return Convert.toWei(mGasPriceGWei, Convert.Unit.GWEI).toBigInteger();
    }

    private void initView() {
        mGasValue = findViewById(R.id.tv_gas_value);
        mGasPriceBar = findViewById(R.id.sb_gas_price);

        mGasPriceBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    Log.d(TAG, "onProgressChanged: " + progress);
                    setGasValueText();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void loadGasPrice() {
        PriceProvider.initOrLoadPrice(new PriceProvider.Callback() {
            @Override
            public void onPriceInfo(PriceProvider.PriceInfo priceInfo) {
                setGasInfo(new BigDecimal(priceInfo.getGasPriceGwei()), new BigDecimal(priceInfo.getEthToYuanRate()));
            }
        });
    }

    private void setGasInfo(BigDecimal gasGWei, BigDecimal ethToYuanRate) {
        mGasPriceGWei = gasGWei;
        mEthToYuanRate = ethToYuanRate;
        if (mGasLimit.compareTo(BigInteger.ZERO) > 0) {
            setGasValueText();
        }
    }

    private void setGasValueText() {
        BigDecimal price = mGasPriceGWei.multiply(new BigDecimal(1 + mGasPriceBar.getProgress() * 9 / 100));
        BigDecimal ethSpend = Util.getEthCost(price, mGasLimit);
        mGasCost = Convert.toWei(ethSpend, Convert.Unit.ETHER).toBigInteger();
        if (mEthToYuanRate.compareTo(BigDecimal.ZERO) == 0) {
            mGasValue.setText(String.format(getContext().getString(R.string.register_gas_value_without_yuan), ethSpend.floatValue()));
        } else {
            double yuan = Util.getYuanCost(price, mGasLimit, mEthToYuanRate);
            mGasValue.setText(String.format(getContext().getString(R.string.register_gas_value), ethSpend.floatValue(), yuan));
        }
    }
}
