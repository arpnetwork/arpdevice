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

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.bean.MinerInfo;
import org.arpnetwork.arpdevice.util.Util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

final class MinerAdapter extends BaseAdapter {
    private Context mContext;
    private List<Miner> mItems;
    private final LayoutInflater mInflater;
    private HashMap<String, Integer> mStateMap;

    public MinerAdapter(Context context) {
        mContext = context;

        mInflater = LayoutInflater.from(context);
        mItems = new ArrayList<Miner>();
        mStateMap = new HashMap<String, Integer>();
    }

    @Override
    public int getCount() {
        return mItems == null ? 0 : mItems.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Miner getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Miner item = mItems.get(position);
        String info = Util.join(mContext.getString(R.string.miner_comma), convertMinerInfo(item.minerInfo));
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_bind_miner, null);

            viewHolder = new ViewHolder();
            viewHolder.mainTitle = (TextView) convertView
                    .findViewById(R.id.tv_miner_name);
            viewHolder.subTitle = (TextView) convertView
                    .findViewById(R.id.tv_miner_info);
            viewHolder.bindState = (TextView) convertView
                    .findViewById(R.id.tv_miner_state);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.mainTitle.setText(item.address);
        viewHolder.subTitle.setText(info);
        if (mStateMap.containsKey(item.address)) {
            viewHolder.bindState.setVisibility(View.VISIBLE);
            int state = mStateMap.get(item.address);
            switch (state) {
                case StateHolder.STATE_BIND_RUNNING:
                    viewHolder.bindState.setText(mContext.getString(R.string.bind_running));
                    break;

                case StateHolder.STATE_BIND_SUCCESS:
                    viewHolder.bindState.setText(mContext.getString(R.string.bind_success));
                    break;

                case StateHolder.STATE_BIND_FAILED:
                    viewHolder.bindState.setText(mContext.getString(R.string.bind_failed));
                    break;

                case StateHolder.STATE_UNBIND_RUNNING:
                    viewHolder.bindState.setText(mContext.getString(R.string.unbind_running));
                    break;

                case StateHolder.STATE_UNBIND_SUCCESS:
                    viewHolder.bindState.setVisibility(View.GONE);
                    break;

                case StateHolder.STATE_UNBIND_FAILED:
                    viewHolder.bindState.setText(mContext.getString(R.string.unbind_failed));
                    break;
            }
        } else {
            viewHolder.bindState.setVisibility(View.GONE);
        }
        return convertView;
    }

    public void setData(List<Miner> items) {
        if (items != null && items.size() > 0) {
            mItems.addAll(items);

            notifyDataSetChanged();
        }
    }

    public void updateLoad(int index, MinerInfo minerInfo) {
        Miner miner = mItems.get(index);
        miner.minerInfo = minerInfo;

        notifyDataSetChanged();
    }

    public boolean isBound(int index) {
        if (index < 0 || index >= mItems.size()) {
            return false;
        }
        Miner miner = mItems.get(index);
        return mStateMap.get(miner.address) != null
                && mStateMap.get(miner.address) == StateHolder.STATE_BIND_SUCCESS;
    }

    public void updateBindState(String address, int state) {
        for (int i = 0; i < mItems.size(); i++) {
            Miner miner = mItems.get(i);
            if (miner.address.equals(address)) {
                mStateMap.put(address, state);

                notifyDataSetChanged();
                break;
            }
        }
    }

    private String[] convertMinerInfo(MinerInfo minerInfo) {
        if (minerInfo == null) return null;
        List<String> items = new LinkedList<>();
        if (!TextUtils.isEmpty(minerInfo.country)) {
            items.add(minerInfo.country);
        }
        BigDecimal bigLoad = new BigDecimal(minerInfo.load);
        double doubleLoad = bigLoad.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

        items.add(String.format(mContext.getString(R.string.miner_load), doubleLoad));
        items.add(String.format(mContext.getString(R.string.miner_bandwidth), minerInfo.bandwidth));
        return items.toArray(new String[items.size()]);
    }

    private static final class ViewHolder {
        TextView mainTitle;
        TextView subTitle;
        TextView bindState;
    }
}
