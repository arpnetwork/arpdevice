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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.app.AtomicNonce;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.config.Constant;
import org.arpnetwork.arpdevice.dialog.MessageDialog;
import org.arpnetwork.arpdevice.server.http.rpc.RPCRequest;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.bean.MinerInfo;
import org.arpnetwork.arpdevice.util.OKHttpUtils;
import org.arpnetwork.arpdevice.util.SimpleCallback;
import org.arpnetwork.arpdevice.util.UIHelper;
import org.web3j.utils.Convert;

import java.math.BigInteger;
import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

import static org.arpnetwork.arpdevice.config.Config.API_SERVER_INFO;

public class MinerListFragment extends BaseFragment {
    private Miner mBoundMiner;

    private ListView mMinerList;
    private MinerAdapter mAdapter;

    private OKHttpUtils mOkHttpUtils;
    private BindStateReceiver mBindStateReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.miner_list);
        if (getArguments() != null) {
            mBoundMiner = (Miner) getArguments().getSerializable(Constant.KEY_MINER);
        }
        mOkHttpUtils = new OKHttpUtils();

        registerReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_miner_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        initViews();
        startLoad();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mOkHttpUtils.cancelTag(API_SERVER_INFO);

        if (mBindStateReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBindStateReceiver);
            mBindStateReceiver = null;
        }
    }

    private void registerReceiver() {
        IntentFilter statusIntentFilter = new IntentFilter(Constant.BROADCAST_ACTION_STATUS);
        statusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        mBindStateReceiver = new BindStateReceiver();
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mBindStateReceiver, statusIntentFilter);
    }

    private void initViews() {
        mAdapter = new MinerAdapter(getContext());
        mMinerList = (ListView) findViewById(R.id.iv_miners);
        mMinerList.setAdapter(mAdapter);
        mMinerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TaskInfo bindingTask = StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING);
                TaskInfo unbindingTask = StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING);
                Miner miner = mAdapter.getItem(position);

                if (bindingTask != null && !miner.getAddress().equals(bindingTask.address)) {
                    if (mBoundMiner != null && mBoundMiner.getAddress().equals(miner.getAddress())) {
                        bindMiner(miner);
                    } else {
                        UIHelper.showToast(getActivity(), R.string.binding_other);
                    }
                } else if (unbindingTask != null && !miner.getAddress().equals(unbindingTask.address)) {
                    UIHelper.showToast(getActivity(), R.string.unbinding_other);
                } else {
                    bindMiner(miner);
                }
            }
        });
    }

    private void bindMiner(final Miner miner) {
        BigInteger unexchanged = null;
        try {
            unexchanged = ARPBank.getUnexchange();
        } catch (Exception e) {
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.network_error)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
            return;
        }
        if (unexchanged.compareTo(BigInteger.ZERO) > 0) {
            String message = String.format(getString(R.string.exchange_change_miner_msg),
                    Convert.fromWei(unexchanged.toString(), Convert.Unit.ETHER).floatValue());
            MessageDialog.Builder builder = new MessageDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.exchange_change_miner_title))
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.exchange), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // todo: show exchange page
                        }
                    })
                    .setNegativeButton(getString(R.string.exchange_change_miner_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Bundle bundle = new Bundle();
                            bundle.putSerializable(Constant.KEY_MINER, miner);
                            startActivity(BindMinerActivity.class, bundle);
                        }
                    })
                    .create()
                    .show();
        } else {
            Bundle bundle = new Bundle();
            bundle.putSerializable(Constant.KEY_MINER, miner);
            startActivity(BindMinerActivity.class, bundle);
        }
    }

    private void startLoad() {
        loadData();
        if (StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING) != null) {
            TaskInfo task = StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING);
            mAdapter.updateBindState(task.address, StateHolder.STATE_BIND_RUNNING);
        } else if (StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING) != null) {
            TaskInfo task = StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING);
            mAdapter.updateBindState(task.address, StateHolder.STATE_UNBIND_RUNNING);
        }
    }

    private void loadData() {
        List<Miner> miners = BindMinerHelper.getMinerList();
        for (int i = 0; i < miners.size(); i++) {
            Miner miner = miners.get(i);
            String url = "http://" + miner.getIpString() + ":" + miner.getPortHttpInt();
            loadMinerLoadInfo(i, url, miner.getAddress());
        }
        mAdapter.setData(miners);
        if (mBoundMiner != null) {
            TaskInfo task = StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING);
            if (task != null) {
                mAdapter.updateBindState(mBoundMiner.getAddress(), StateHolder.STATE_UNBIND_RUNNING);
                mAdapter.updateBindState(task.address, StateHolder.STATE_BIND_RUNNING);
            } else {
                mAdapter.updateBindState(mBoundMiner.getAddress(), StateHolder.STATE_BIND_SUCCESS);
            }
        }
    }

    private void loadMinerLoadInfo(final int index, final String url, String minerAddr) {
        String nonce = AtomicNonce.getAndIncrement(minerAddr);

        RPCRequest request = new RPCRequest();
        request.setId(nonce);
        request.setMethod(API_SERVER_INFO);

        mOkHttpUtils.post(url, request.toJSON(), API_SERVER_INFO, new SimpleCallback<MinerInfo>() {
            @Override
            public void onFailure(Request request, Exception e) {
            }

            @Override
            public void onSuccess(Response response, MinerInfo result) {
                mAdapter.updateLoad(index, result);
            }

            @Override
            public void onError(Response response, int code, Exception e) {
            }
        });
    }

    private class BindStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(Constant.EXTENDED_DATA_STATUS, StateHolder.STATE_APPROVE_RUNNING)) {
                case StateHolder.STATE_BIND_RUNNING:
                    if (mBoundMiner != null && mBoundMiner.getAddress() != null) {
                        mAdapter.updateBindState(mBoundMiner.getAddress(), StateHolder.STATE_UNBIND_RUNNING);
                    }
                    TaskInfo task = StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING);
                    mAdapter.updateBindState(task.address, StateHolder.STATE_BIND_RUNNING);
                    break;

                case StateHolder.STATE_BIND_SUCCESS:
                    UIHelper.showToast(getActivity(), getString(R.string.bind_success));

                    if (mBoundMiner != null) {
                        mAdapter.removeState(mBoundMiner.getAddress());
                    }

                    TaskInfo taskSuccess = StateHolder.getTaskByState(StateHolder.STATE_BIND_SUCCESS);
                    mAdapter.updateBindState(taskSuccess.address, StateHolder.STATE_BIND_SUCCESS);

                    mBoundMiner = new Miner();
                    mBoundMiner.setAddress(taskSuccess.address);
                    break;

                case StateHolder.STATE_BIND_FAILED:
                    UIHelper.showToast(getActivity(), getString(R.string.bind_failed));

                    TaskInfo taskFailed = StateHolder.getTaskByState(StateHolder.STATE_BIND_FAILED);
                    mAdapter.updateBindState(taskFailed.address, StateHolder.STATE_BIND_FAILED);
                    break;

                case StateHolder.STATE_UNBIND_RUNNING:
                    TaskInfo unbindTask = StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING);
                    mAdapter.updateBindState(unbindTask.address, StateHolder.STATE_UNBIND_RUNNING);
                    break;

                case StateHolder.STATE_UNBIND_SUCCESS:
                    UIHelper.showToast(getActivity(), getString(R.string.unbind_success));

                    mBoundMiner = null;
                    finish();
                    break;

                case StateHolder.STATE_UNBIND_FAILED:
                    TaskInfo unbindFailed = StateHolder.getTaskByState(StateHolder.STATE_UNBIND_FAILED);
                    mAdapter.updateBindState(unbindFailed.address, StateHolder.STATE_UNBIND_FAILED);
                    break;

                default:
                    break;
            }
        }
    }
}
