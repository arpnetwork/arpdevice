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
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.util.Util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EarningDetailsItem extends LinearLayout {
    private TextView mDateView;
    private TextView mIncomeView;
    private View mDivider;
    private LinearLayout mItemLayout;

    public EarningDetailsItem(Context context) {
        super(context);

        init();
    }

    public EarningDetailsItem(Context context, AttributeSet attr) {
        super(context, attr);

        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.item_earning, this, true);

        mDateView = findViewById(R.id.tv_earning_date);
        mIncomeView = findViewById(R.id.tv_earning_income);
        mDivider = findViewById(R.id.divider);
        mItemLayout = findViewById(R.id.layout_item);
    }

    public void setData(Earning earning) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss");
        mDateView.setText(format.format(new Date(earning.time)));
        mIncomeView.setText(String.format(getResources().getString(R.string.earning_detail_format), earning.earning, earning.minerAddress));
    }

    public void setFirstOrLastItem(boolean firstItem, boolean lastItem) {
        if (firstItem) {
            mDivider.setVisibility(GONE);
            mItemLayout.setBackgroundResource(R.drawable.round_corner_bg_top);
        } else {
            mDivider.setVisibility(VISIBLE);
            if (lastItem) {
                mItemLayout.setBackgroundResource(R.drawable.round_corner_bg_bottom);
            } else {
                mItemLayout.setBackgroundColor(Color.WHITE);
            }
        }
    }
}
