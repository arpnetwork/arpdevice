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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

final class MinerAdapter extends BaseAdapter {
    private Context mContext;
    private List<Miner> mitems;
    private final LayoutInflater mInflater;
    private HashMap<String, Integer> mStateMap;

    public MinerAdapter(Context context) {
        mContext = context;

        mInflater = LayoutInflater.from(context);
        mitems = new ArrayList<Miner>();
        mStateMap = new HashMap<String, Integer>();
    }

    @Override
    public int getCount() {
        return mitems == null ? 0 : mitems.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Miner getItem(int position) {
        return mitems.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Miner item = mitems.get(position);
        String info = item.country + (TextUtils.isEmpty(item.load) ? "" : mContext.getString(R.string.miner_load) + item.load) +
                mContext.getString(R.string.miner_bandwidth) + item.bandwidth;
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

    public void setData(List<Miner> datas) {
        if (datas != null && datas.size() > 0) {
            mitems.addAll(datas);

            notifyDataSetChanged();
        }
    }

    public void updateLoad(int index, String load) {
        Miner miner = mitems.get(index);
        miner.load = load;

        notifyDataSetChanged();
    }

    public boolean isBound(int index) {
        if (index < 0 || index >= mitems.size()) {
            return false;
        }
        Miner miner = mitems.get(index);
        return mStateMap.get(miner.address) != null
                && mStateMap.get(miner.address) == StateHolder.STATE_BIND_SUCCESS;
    }

    public void updateBindState(String address, int state) {
        for (int i = 0; i < mitems.size(); i++) {
            Miner miner = mitems.get(i);
            if (miner.address.equals(address)) {
                mStateMap.put(address, state);

                notifyDataSetChanged();
                break;
            }
        }
    }

    private static final class ViewHolder {
        TextView mainTitle;
        TextView subTitle;
        TextView bindState;
    }
}
