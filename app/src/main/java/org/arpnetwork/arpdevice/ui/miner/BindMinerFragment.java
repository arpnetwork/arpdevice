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

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.contract.BalanceAPI;
import org.arpnetwork.arpdevice.contract.tasks.OnValueResult;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.wallet.WalletManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BindMinerFragment extends BaseFragment {
    private static final String TAG = "BindMinerFragment";

    private static final int LOCK_ARP = 500;

    private ListView mMinerList;
    private MinerAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.bind_miner);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bind_miner, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews();
        loadData();
    }

    private void initViews() {
        mMinerList = (ListView) findViewById(R.id.iv_miners);
        mAdapter = new MinerAdapter(getContext());
        mMinerList.setAdapter(mAdapter);
        mMinerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!mAdapter.isChecked(position)) {
                    // TODO: show gas price dialog
                }
            }
        });
    }

    protected void loadData() {
        List<Miner> datas = new ArrayList<>();
        mAdapter.setData(datas);
    }

    interface Callback {
        void onResult(String result, int index);
    }

    private static class MinerLoadTask extends AsyncTask<String, Void, String> {
        private Callback func;
        private int index;

        public MinerLoadTask(int index, Callback func) {
            this.func = func;
            this.index = index;
        }

        @Override
        protected String doInBackground(String... addresses) {
            String address = addresses[0];
            return address;
        }

        @Override
        protected void onPostExecute(String result) {
            if (!isCancelled() && func != null && !TextUtils.isEmpty(result)) {
                func.onResult(result, index);
            }
        }
    }

    private static final class MinerAdapter extends BaseAdapter {
        private Context mContext;
        private List<Miner> mitems;
        private final LayoutInflater mInflater;
        private int mCheckedIndex = -1;

        public MinerAdapter(Context context) {
            mContext = context;

            mInflater = LayoutInflater.from(context);
            mitems = new ArrayList<Miner>();
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
            String info = item.country + "，" + mContext.getString(R.string.miner_load) + item.load +
                    "，" + mContext.getString(R.string.miner_bandwidth) + item.bandwidth;
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

            viewHolder.mainTitle.setText(item.name);
            viewHolder.subTitle.setText(info);
            if (item.binded || mCheckedIndex == position) {
                viewHolder.bindState.setVisibility(View.VISIBLE);
                viewHolder.bindState.setText(mContext.getString(R.string.bind_success));
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

        public void checkItem(int index) {
            if (index >= 0) {
                mCheckedIndex = index;

                notifyDataSetChanged();
            }
        }

        public boolean isChecked(int index) {
            return mCheckedIndex >= 0 && mCheckedIndex == index;
        }

        public void updateLoad(int index, String load) {
            Miner miner = mitems.get(index);
            miner.load = load;

            notifyDataSetChanged();
        }

        private static final class ViewHolder {
            TextView mainTitle;
            TextView subTitle;
            TextView bindState;
        }
    }

    private void checkBalance(final double gasPrice) {
        // check balance before binding miner
        final String address = WalletManager.getInstance().getWallet().getPublicKey();
        BalanceAPI.getEtherBalance(address, new OnValueResult() {
            @Override
            public void onValueResult(BigDecimal result) {
                if (result.doubleValue() < gasPrice) {
                    showErrorAlertDialog(null, getString(R.string.bind_miner_error_balance_insufficient));
                } else {
                    BalanceAPI.getArpBalance(address, new OnValueResult() {
                        @Override
                        public void onValueResult(BigDecimal result) {
                            if (result.doubleValue() < LOCK_ARP) {
                                showErrorAlertDialog(null, getString(R.string.bind_miner_error_balance_insufficient));
                            } else {
                                // TODO: bind miner
                            }
                        }
                    });
                }
            }
        });
    }

    private void showErrorAlertDialog(String title, String message) {
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(false)
                .show();
    }
}