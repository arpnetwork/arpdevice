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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.app.AtomicNonce;
import org.arpnetwork.arpdevice.config.Constant;
import org.arpnetwork.arpdevice.contracts.tasks.AbsExceptionTask;
import org.arpnetwork.arpdevice.contracts.tasks.OnValueResult;
import org.arpnetwork.arpdevice.contracts.tasks.SimpleOnValueResult;
import org.arpnetwork.arpdevice.dialog.PromiseDialog;
import org.arpnetwork.arpdevice.server.http.rpc.RPCRequest;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.bean.MinerInfo;
import org.arpnetwork.arpdevice.ui.order.details.ExchangeActivity;
import org.arpnetwork.arpdevice.ui.widget.EmptyView;
import org.arpnetwork.arpdevice.util.OKHttpUtils;
import org.arpnetwork.arpdevice.util.SimpleCallback;
import org.arpnetwork.arpdevice.util.UIHelper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.Request;
import okhttp3.Response;

import static org.arpnetwork.arpdevice.config.Config.API_SERVER_INFO;
import static org.arpnetwork.arpdevice.config.Constant.KEY_EXCHANGE_AMOUNT;
import static org.arpnetwork.arpdevice.config.Constant.KEY_EXCHANGE_TYPE;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerIntentService.OPERATION_CASH;

public class MinerListFragment extends BaseFragment {
    private static final int PING_INTERVAL = 500;
    private static final int PING_COUNT = 3;
    private static final int UNREACHABLE_TIME_MS = 60 * 1000;

    private Map<Integer, List<Integer>> mPings = new HashMap<Integer, List<Integer>>();
    private Thread mPingThread;

    private Miner mBoundMiner;

    private ListView mMinerList;
    private MinerAdapter mAdapter;
    private ListLoadTask mListLoader;

    private OKHttpUtils mOkHttpUtils;
    private BindStateReceiver mBindStateReceiver;
    private boolean mIgnoreExchage;

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

        if (mPingThread != null) {
            mPingThread.interrupt();
            mPingThread = null;
        }

        mOkHttpUtils.cancelTag(API_SERVER_INFO);

        cancelPotentialTask();

        if (mBindStateReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBindStateReceiver);
            mBindStateReceiver = null;
        }
    }

    @Override
    protected void loadData() {
        cancelPotentialTask();

        mListLoader = new ListLoadTask(new SimpleOnValueResult<List<Miner>>() {
            @Override
            public void onValueResult(List<Miner> result) {
                if (result != null && result.size() > 0) {
                    showContentView();

                    for (int i = 0; i < result.size(); i++) {
                        Miner miner = result.get(i);
                        String url = "http://" + miner.getIpString() + ":" + miner.getPortHttpInt();
                        loadMinerLoadInfo(i, url, miner.getAddress());
                    }
                    loadMinerPingAsync(result);

                    mAdapter.setData(result);
                    if (mBoundMiner != null) {
                        TaskInfo task = StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING);
                        if (task != null) {
                            mAdapter.updateBindState(mBoundMiner.getAddress(), StateHolder.STATE_UNBIND_RUNNING);
                            mAdapter.updateBindState(task.address, StateHolder.STATE_BIND_RUNNING);
                        } else {
                            mAdapter.updateBindState(mBoundMiner.getAddress(), StateHolder.STATE_BIND_SUCCESS);
                        }
                    }

                    if (StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING) != null) {
                        TaskInfo task = StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING);
                        mAdapter.updateBindState(task.address, StateHolder.STATE_BIND_RUNNING);
                    } else if (StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING) != null) {
                        TaskInfo task = StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING);
                        mAdapter.updateBindState(task.address, StateHolder.STATE_UNBIND_RUNNING);
                    }
                } else {
                    showEmptyView(EmptyView.STATE_EMPTY);
                }
            }

            @Override
            public void onFail(Throwable throwable) {
                showEmptyView(EmptyView.STATE_FAIL);
            }
        });
        mListLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected View getContentView() {
        return mMinerList;
    }

    private void cancelPotentialTask() {
        if (mListLoader != null) {
            mListLoader.cancel(true);
            mListLoader = null;
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
                Miner miner = mAdapter.getItem(position);
                if (mBoundMiner != null && mBoundMiner.getAddress().equals(miner.getAddress())) {
                    showMinerPage(miner);
                    return;
                }

                TaskInfo bindingTask = StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING);
                TaskInfo unbindingTask = StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING);

                if (bindingTask != null && !miner.getAddress().equals(bindingTask.address)) {
                    UIHelper.showToast(getActivity(), R.string.binding_other);
                } else if (unbindingTask != null && !miner.getAddress().equals(unbindingTask.address)) {
                    UIHelper.showToast(getActivity(), R.string.unbinding_other);
                } else {
                    if (mIgnoreExchage) {
                        showMinerPage(miner);
                    } else {
                        checkPromise(miner);
                    }
                }
            }
        });
    }

    private void checkPromise(final Miner miner) {
        PromiseDialog.show(getContext(), R.string.exchange_change_miner_msg, getString(R.string.exchange_change_miner_ignore),
                new PromiseDialog.PromiseListener() {
                    @Override
                    public void onError() {
                        if (getActivity() == null) return;

                        finish();
                    }

                    @Override
                    public void onExchange(BigInteger unexchanged) {
                        if (getActivity() == null) return;

                        Bundle bundle = new Bundle();
                        bundle.putInt(KEY_EXCHANGE_TYPE, OPERATION_CASH);
                        bundle.putString(KEY_EXCHANGE_AMOUNT, unexchanged.toString());
                        bundle.putSerializable(Constant.KEY_MINER, miner);
                        startActivity(ExchangeActivity.class, bundle);
                    }

                    @Override
                    public void onIgnore() {
                        if (getActivity() == null) return;

                        showMinerPage(miner);
                        mIgnoreExchage = true;
                    }
                });
    }

    private void showMinerPage(Miner miner) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constant.KEY_MINER, miner);
        startActivity(BindMinerActivity.class, bundle);
    }

    private void startLoad() {
        loadData();
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

    private void loadMinerPingAsync(final List<Miner> miners) {
        mPingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    loadMinerPing(miners);
                } catch (InterruptedException ignored) {
                }
            }
        });
        mPingThread.start();
    }

    private void loadMinerPing(List<Miner> miners) throws InterruptedException {
        for (int i = 0; i < miners.size(); i++) {
            Miner miner = miners.get(i);
            String url = "http://" + miner.getIpString() + ":" + miner.getPortHttpInt();
            for (int j = 0; j < PING_COUNT; j++) {
                Thread.sleep(PING_INTERVAL);
                loadPingInternal(i, url, miner.getAddress());
            }
        }
    }

    private void loadPingInternal(final int index, final String url, String minerAddr) {
        String nonce = AtomicNonce.getAndIncrement(minerAddr);

        RPCRequest request = new RPCRequest();
        request.setId(nonce);
        request.setMethod(API_SERVER_INFO);

        final long start = System.currentTimeMillis();

        mOkHttpUtils.post(url, request.toJSON(), API_SERVER_INFO, new SimpleCallback<MinerInfo>() {
            @Override
            public void onFailure(Request request, Exception e) {
                pingReachable(index, start, false);
            }

            @Override
            public void onSuccess(Response response, MinerInfo result) {
                pingReachable(index, start, true);
            }

            @Override
            public void onError(Response response, int code, Exception e) {
                pingReachable(index, start, true);
            }
        });
    }

    private void pingReachable(int index, long start, boolean reachable) {
        if (mPings.containsKey(index)) {
            List<Integer> list = mPings.get(index);
            if (reachable) {
                list.add((int) (System.currentTimeMillis() - start) / 2);
            } else {
                list.add(UNREACHABLE_TIME_MS);
            }
            if (list.size() == PING_COUNT) {
                for (Iterator<Map.Entry<Integer, List<Integer>>> it = mPings.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<Integer, List<Integer>> item = it.next();
                    if (item.getKey() == index) {
                        int total = 0;
                        int unreachableCount = 0;
                        List<Integer> pings = item.getValue();
                        for (Integer ping : pings) {
                            if (ping == UNREACHABLE_TIME_MS) {
                                unreachableCount += 1;
                            }
                            total += ping;
                        }
                        int avg;
                        if (unreachableCount == pings.size()) {
                            avg = -1;
                        } else {
                            avg = total / pings.size();
                        }
                        mAdapter.updatePing(index, avg);

                        it.remove();
                        break;
                    }
                }
            }
        } else {
            List<Integer> list = new ArrayList<>();
            if (reachable) {
                list.add((int) (System.currentTimeMillis() - start) / 2);
            } else {
                list.add(UNREACHABLE_TIME_MS);
            }
            mPings.put(index, list);
        }
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

    private static class ListLoadTask extends AbsExceptionTask<Void, Void, List<Miner>> {
        public ListLoadTask(OnValueResult<List<Miner>> onValueResult) {
            super(onValueResult);
        }

        @Override
        public List<Miner> onInBackground(Void... voids) throws Exception {
            return BindMinerHelper.getMinerList();
        }
    }
}
