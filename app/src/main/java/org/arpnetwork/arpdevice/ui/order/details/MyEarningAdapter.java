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
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.arpnetwork.arpdevice.database.EarningRecord;

import java.util.ArrayList;
import java.util.List;

public class MyEarningAdapter extends BaseAdapter {
    private Context mContext;
    private List<EarningRecord> mList;

    public MyEarningAdapter(Context context) {
        mContext = context;
        mList = new ArrayList<EarningRecord>();
    }

    public void setData(List<EarningRecord> list) {
        mList.clear();
        mList.addAll(list);
        notifyDataSetChanged();
    }

    public void addData(List<EarningRecord> list) {
        mList.addAll(0, list);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public EarningRecord getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        EarningDetailsItem item;
        if (convertView == null) {
            item = new EarningDetailsItem(mContext);
        } else {
            item = (EarningDetailsItem) convertView;
        }
        item.setData(getItem(position));
        item.setFirstOrLastItem(position == 0, position == getCount() - 1);
        return item;
    }
}
