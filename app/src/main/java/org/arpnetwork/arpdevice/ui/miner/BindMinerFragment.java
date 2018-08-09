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
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.contracts.ARPContract;
import org.arpnetwork.arpdevice.contracts.api.BalanceAPI;
import org.arpnetwork.arpdevice.contracts.tasks.OnValueResult;
import org.arpnetwork.arpdevice.config.Constant;
import org.arpnetwork.arpdevice.contracts.tasks.BindMinerHelper;
import org.arpnetwork.arpdevice.dialog.PasswordDialog;
import org.arpnetwork.arpdevice.dialog.SeekBarDialog;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.bean.GasInfo;
import org.arpnetwork.arpdevice.ui.bean.GasInfoResponse;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.util.OKHttpUtils;
import org.arpnetwork.arpdevice.util.SimpleCallback;
import org.arpnetwork.arpdevice.util.UIHelper;
import org.arpnetwork.arpdevice.util.Util;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.Credentials;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

import static org.arpnetwork.arpdevice.config.Constant.KEY_ADDRESS;
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASLIMIT;
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASPRICE;
import static org.arpnetwork.arpdevice.config.Constant.KEY_OP;
import static org.arpnetwork.arpdevice.config.Constant.KEY_PASSWD;

public class BindMinerFragment extends BaseFragment {
    private static final String TAG = "BindMinerFragment";

    public static final int OPERATION_APPROVE = 1;
    public static final int OPERATION_BIND = 2;
    public static final int OPERATION_UNBOUND = 3;

    private static final int LOCK_ARP = 500;

    private ListView mMinerList;
    private TextView mBandTipTextView;
    private MinerAdapter mAdapter;
    private View mApproveView;
    private TextView mApproveTextView;
    private OKHttpUtils mOkHttpUtils;

    private Miner mBindMiner;
    private String mApprovedPasswd; // If passwd for approve first then bind does't need.
    private boolean mNeedApprove;

    private int mClickPosition;
    private GasInfo mGasInfo;
    private BigDecimal mGasPriceGWei;
    private Dialog mShowPriceDialog;
    private Dialog mInputPasswdDialog;

    private BindStateReceiver mBindStateReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.bind_miner);

        mOkHttpUtils = new OKHttpUtils();

        registerReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bind_miner, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        initViews();

        if (StateHolder.getTaskByState(StateHolder.STATE_APPROVE_RUNNING) != null) {
            showApproveView(R.string.bind_approving);
        } else if (StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING) != null) {
            loadData();
            TaskInfo task = StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING);
            mAdapter.updateBindState(task.address, StateHolder.STATE_BIND_RUNNING);
        } else {
            loadBindState();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mBindStateReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBindStateReceiver);
            mBindStateReceiver = null;
        }
    }

    private void registerReceiver() {
        IntentFilter statusIntentFilter = new IntentFilter(
                Constant.BROADCAST_ACTION);
        statusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        mBindStateReceiver = new BindStateReceiver();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mBindStateReceiver,
                statusIntentFilter);
    }

    private void initViews() {
        mMinerList = (ListView) findViewById(R.id.iv_miners);
        mBandTipTextView = (TextView) findViewById(R.id.tv_bind_tip);
        mApproveView = findViewById(R.id.layout_approve);
        mApproveTextView = (TextView) mApproveView.findViewById(R.id.tv_approve);

        mAdapter = new MinerAdapter(getContext());
        mMinerList.setAdapter(mAdapter);
        mMinerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING) != null) {
                    return;
                }
                if (!mAdapter.isBinded(position)) {
                    mClickPosition = position;
                    if (mBindMiner != null || StateHolder.getTaskByState(StateHolder.STATE_BIND_SUCCESS) != null) {
                        // TODO: change bind unbound.
                        // unbound success  then loadGasInfo();
                    } else {
                        loadGasInfo();
                    }
                }
            }
        });
    }

    private void showApproveView(int stateTextId) {
        mApproveTextView.setText(stateTextId);
        mApproveView.setVisibility(View.VISIBLE);

        mBandTipTextView.setVisibility(View.GONE);
        mMinerList.setVisibility(View.GONE);
    }

    private void showListView() {
        mApproveView.setVisibility(View.GONE);

        mBandTipTextView.setVisibility(View.VISIBLE);
        mMinerList.setVisibility(View.VISIBLE);
    }

    private void loadBindState() {
        String address = Wallet.get().getPublicKey();
        Miner miner = BindMinerHelper.getBinded(address);
        if (miner != null) {
            mBindMiner = miner;
            loadData();
        } else {
            loadAllowance();
        }
    }

    private void loadAllowance() {
        String owner = Wallet.get().getPublicKey();
        String spender = BindMinerHelper.CONTRACT_ADDRESS;
        ARPContract.allowanceARP(owner, spender, new OnValueResult<BigDecimal>() {
            @Override
            public void onValueResult(BigDecimal result) {
                if (result != null && result.intValue() >= LOCK_ARP) {
                    loadData();
                } else {
                    mNeedApprove = true;
                    loadGasInfo();
                }
            }
        });
    }

    private void loadData() {
        showListView();

        List<Miner> miners = BindMinerHelper.getMinerList();
        for (int i = 0; i < miners.size(); i++) {
            Miner miner = miners.get(i);
            String url = "http://" + Util.longToIp(miner.ip.longValue()) + ":" + miner.port.intValue();
            loadMinerLoadInfo(i, url);
        }
        mAdapter.setData(miners);
        if (mBindMiner != null) {
            mAdapter.updateBindState(mBindMiner.address, StateHolder.STATE_BIND_SUCCESS);
        }
    }

    private void loadMinerLoadInfo(final int index, final String url) {
        mOkHttpUtils.get(url, new SimpleCallback<String>() {
            @Override
            public void onFailure(Request request, Exception e) {
            }

            @Override
            public void onSuccess(Response response, String result) {
                try {
                    JSONObject jsonObject = new JSONObject(result); // {"load":"70%"}
                    String load = jsonObject.optString("load");
                    mAdapter.updateLoad(index, load);
                } catch (JSONException ignored) {
                }
            }

            @Override
            public void onError(Response response, int code, Exception e) {
            }
        });
    }

    private void loadGasInfo() {
        mOkHttpUtils.get(Constant.API_URL, new SimpleCallback<GasInfoResponse>() {
            @Override
            public void onFailure(Request request, Exception e) {
                if (getActivity() != null) {
                    UIHelper.showToast(getActivity(), getString(R.string.load_gas_failed));
                }
            }

            @Override
            public void onSuccess(Response response, GasInfoResponse result) {
                mGasInfo = result.data;
                if (mNeedApprove) {
                    showApproveEthDialog(result.data);
                } else {
                    checkBalance(Util.getEthCost(result.data.getGasPriceGwei(), result.data.getGasLimit()).doubleValue());
                }
            }

            @Override
            public void onError(Response response, int code, Exception e) {
                if (getActivity() != null) {
                    UIHelper.showToast(getActivity(), getString(R.string.load_gas_failed));
                }
            }
        });
    }

    private void checkBalance(final double ethCost) {
        // check balance before binding miner
        final String address = Wallet.get().getPublicKey();
        BalanceAPI.getEtherBalance(address, new OnValueResult<BigDecimal>() {
            @Override
            public void onValueResult(BigDecimal result) {
                if (result.doubleValue() < ethCost) {
                    showErrorAlertDialog(null, getString(R.string.bind_miner_error_balance_insufficient));
                } else {
                    BalanceAPI.getArpBalance(address, new OnValueResult<BigDecimal>() {
                        @Override
                        public void onValueResult(BigDecimal result) {
                            if (result.doubleValue() < LOCK_ARP) {
                                showErrorAlertDialog(null, getString(R.string.bind_miner_error_balance_insufficient));
                            } else {
                                showPayEthDialog(mGasInfo);
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

    private void showApproveEthDialog(final GasInfo data) {
        final BigDecimal min = data.getGasPriceGwei();
        final BigDecimal max = (data.getGasPriceGwei().multiply(new BigDecimal("100")));
        BigDecimal defaultValue = data.getGasPriceGwei();
        mGasPriceGWei = data.getGasPriceGwei();

        final BigDecimal mEthSpend = Util.getEthCost(data.getGasPriceGwei(), data.getGasLimit());
        double yuan = Util.getYuanCost(defaultValue, data.getGasLimit(), data.getEthToYuanRate());

        final SeekBarDialog.Builder builder = new SeekBarDialog.Builder(getContext());
        builder.setTitle(getString(R.string.bind_btn_approve))
                .setMessage(getString(R.string.bind_approve_message))
                .setSeekValue(0, String.format(getString(R.string.bind_eth_format), mEthSpend, yuan))
                .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            // min + progress * (max - min) / 100;
                            BigDecimal multiply = new BigDecimal(progress).multiply(max.subtract(min));
                            BigDecimal divide = multiply.divide(new BigDecimal("100"));
                            mGasPriceGWei = min.add(divide);
                            BigDecimal mEthSpend = Util.getEthCost(mGasPriceGWei, data.getGasLimit());
                            double yuan = Util.getYuanCost(mGasPriceGWei, data.getGasLimit(), data.getEthToYuanRate());
                            builder.setSeekValue(progress, String.format(getString(R.string.bind_eth_format), mEthSpend, yuan));
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                })
                .setPositiveButton(getString(R.string.bind_btn_approve), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showInputPasswdDialog(OPERATION_APPROVE);
                    }
                })
                .setNegativeButton(getString(R.string.bind_btn_quit), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finish();
                    }
                })
                .create()
                .show();
    }

    private void showPayEthDialog(final GasInfo data) {
        if (mShowPriceDialog != null && mShowPriceDialog.isShowing()) return;

        Miner minerInfo = mAdapter.getItem(mClickPosition);
        final BigDecimal min = data.getGasPriceGwei();
        final BigDecimal max = (data.getGasPriceGwei().multiply(new BigDecimal("100")));
        BigDecimal defaultValue = data.getGasPriceGwei();
        mGasPriceGWei = data.getGasPriceGwei();

        final BigDecimal mEthSpend = Util.getEthCost(data.getGasPriceGwei(), data.getGasLimit());
        double yuan = Util.getYuanCost(defaultValue, data.getGasLimit(), data.getEthToYuanRate());

        final SeekBarDialog.Builder builder = new SeekBarDialog.Builder(getContext());
        builder.setTitle(minerInfo.address)
                .setMessage(getString(R.string.bind_message))
                .setSeekValue(0, String.format(getString(R.string.bind_eth_format), mEthSpend, yuan))
                .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            // min + progress * (max - min) / 100;
                            BigDecimal multiply = new BigDecimal(progress).multiply(max.subtract(min));
                            BigDecimal divide = multiply.divide(new BigDecimal("100"));
                            mGasPriceGWei = min.add(divide);
                            BigDecimal mEthSpend = Util.getEthCost(mGasPriceGWei, data.getGasLimit());
                            double yuan = Util.getYuanCost(mGasPriceGWei, data.getGasLimit(), data.getEthToYuanRate());
                            builder.setSeekValue(progress, String.format(getString(R.string.bind_eth_format), mEthSpend, yuan));
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                })
                .setPositiveButton(getString(R.string.bind_btn_bind), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mApprovedPasswd != null) {
                            dialog.dismiss();
                            startBindService(mApprovedPasswd, OPERATION_BIND);
                        } else {
                            showInputPasswdDialog(OPERATION_BIND);
                        }
                    }
                })
                .setNegativeButton(getString(R.string.bind_btn_quit), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finish();
                    }
                });
        mShowPriceDialog = builder.create();
        mShowPriceDialog.show();
    }

    private void showInputPasswdDialog(final int opType) {
        if (mInputPasswdDialog != null && mInputPasswdDialog.isShowing()) return;

        final PasswordDialog.Builder builder = new PasswordDialog.Builder(getContext());
        builder.setOnClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String passwd = builder.getPassword();
                if (TextUtils.isEmpty(passwd)) {
                    UIHelper.showToast(getActivity(), getString(R.string.input_passwd_tip));
                } else {
                    final Credentials credentials = Wallet.loadCredentials(passwd);
                    if (credentials == null) {
                        UIHelper.showToast(getActivity(), getString(R.string.input_passwd_error));
                    } else {
                        dialog.dismiss();
                        if (opType == OPERATION_APPROVE) {
                            mApprovedPasswd = passwd;
                            startApproveService(passwd, OPERATION_APPROVE);
                        } else if (opType == OPERATION_BIND) {
                            startBindService(passwd, OPERATION_BIND);
                        }
                    }
                }
            }
        });
        mInputPasswdDialog = builder.create();
        mInputPasswdDialog.show();
    }

    private void startBindService(String passwd, int opType) {
        Intent mServiceIntent = new Intent(getActivity(), BindMinerIntentService.class);
        mServiceIntent.putExtra(KEY_OP, opType);
        mServiceIntent.putExtra(KEY_PASSWD, passwd);
        mServiceIntent.putExtra(KEY_ADDRESS, mAdapter.getItem(mClickPosition).address);
        mServiceIntent.putExtra(KEY_GASPRICE, getGasPrice().toString());
        mServiceIntent.putExtra(KEY_GASLIMIT, mGasInfo.getGasLimit().toString());
        getActivity().startService(mServiceIntent);
    }

    private void startApproveService(String passwd, int opType) {
        Intent mServiceIntent = new Intent(getActivity(), BindMinerIntentService.class);
        mServiceIntent.putExtra(KEY_OP, opType);
        mServiceIntent.putExtra(KEY_PASSWD, passwd);
        mServiceIntent.putExtra(KEY_GASPRICE, getGasPrice().toString());
        getActivity().startService(mServiceIntent);
    }

    private BigInteger getGasPrice() {
        BigDecimal gas = Convert.toWei(mGasPriceGWei, Convert.Unit.GWEI);
        return gas.toBigInteger();
    }

    private class BindStateReceiver extends BroadcastReceiver {
        private BindStateReceiver() {
            // prevents instantiation by other packages.
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(Constant.EXTENDED_DATA_STATUS,
                    StateHolder.STATE_APPROVE_RUNNING)) {
                case StateHolder.STATE_APPROVE_RUNNING:
                    showApproveView(R.string.bind_approving);
                    break;

                case StateHolder.STATE_APPROVE_SUCCESS:
                    UIHelper.showToast(getActivity(), getString(R.string.bind_approve_success));
                    loadData();
                    mNeedApprove = false;
                    break;

                case StateHolder.STATE_APPROVE_FAILED:
                    showApproveView(R.string.bind_approve_failed);
                    mApprovedPasswd = null;
                    break;

                case StateHolder.STATE_BIND_RUNNING:
                    mApprovedPasswd = null;

                    TaskInfo task = StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING);
                    mAdapter.updateBindState(task.address, StateHolder.STATE_BIND_RUNNING);
                    break;

                case StateHolder.STATE_BIND_SUCCESS:
                    mApprovedPasswd = null;
                    UIHelper.showToast(getActivity(), getString(R.string.bind_success));

                    TaskInfo taskSuccess = StateHolder.getTaskByState(StateHolder.STATE_BIND_SUCCESS);
                    mAdapter.updateBindState(taskSuccess.address, StateHolder.STATE_BIND_SUCCESS);
                    break;

                case StateHolder.STATE_BIND_FAILED:
                    UIHelper.showToast(getActivity(), getString(R.string.bind_failed));

                    TaskInfo taskFailed = StateHolder.getTaskByState(StateHolder.STATE_BIND_FAILED);
                    mAdapter.updateBindState(taskFailed.address, StateHolder.STATE_BIND_FAILED);
                    break;

                default:
                    break;
            }
        }
    }
}