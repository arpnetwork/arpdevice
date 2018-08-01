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

package org.arpnetwork.arpdevice.ui.order;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.arpnetwork.arpdevice.R;

public class OrderDetailsHeader extends LinearLayout {
    private TextView mTotalIncome;
    private TextView mTodayIncome;

    public OrderDetailsHeader(Context context) {
        super(context);

        init();
    }

    public OrderDetailsHeader(Context context, AttributeSet attr) {
        super(context, attr);

        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.header_order_details, this, true);

        mTotalIncome = findViewById(R.id.tv_totol_income);
        mTodayIncome = findViewById(R.id.tv_today_income);
    }

    public void setData(OrderData data) {
        mTotalIncome.setText(String.format(getResources().getString(R.string.total_income), data.totalIncome));
        mTodayIncome.setText(String.format(getResources().getString(R.string.today_income), data.todayIncome));
    }
}
