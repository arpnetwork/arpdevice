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

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.arpnetwork.arpdevice.R;

public class MyEarningHeader extends LinearLayout {
    private TextView mUnexchanged;
    private TextView mExchanged;
    private Button mExchangeBtn;

    public MyEarningHeader(Context context, OnClickListener onClickListener) {
        this(context, null, onClickListener);
    }

    public MyEarningHeader(Context context, AttributeSet attr, OnClickListener onClickListener) {
        super(context, attr);

        init(onClickListener);
    }

    private void init(View.OnClickListener onClickListener) {
        LayoutInflater.from(getContext()).inflate(R.layout.header_my_earning, this, true);

        mUnexchanged = findViewById(R.id.tv_unexchanged);
        mUnexchanged.setText(String.format(getResources().getString(R.string.unexchanged), 0.0));

        mExchanged = findViewById(R.id.tv_exchanged);
        mExchanged.setText(String.format(getResources().getString(R.string.exchanged), 0.0));

        mExchangeBtn = findViewById(R.id.btn_exchange);
        mExchangeBtn.setOnClickListener(onClickListener);
    }

    public void setData(EarningData data) {
        mUnexchanged.setText(String.format(getResources().getString(R.string.unexchanged), data.unexchanged));
        mExchanged.setText(String.format(getResources().getString(R.string.exchanged), data.exchanged));
        TextView recordTitle = findViewById(R.id.tv_record);
        if (data.earningList != null && data.earningList.size() > 0) {
            recordTitle.setVisibility(VISIBLE);
        } else {
            recordTitle.setVisibility(GONE);
        }
    }

    public void setUnexchanged(float unexchanged) {
        mUnexchanged.setText(String.format(getResources().getString(R.string.unexchanged), unexchanged));
    }
}
