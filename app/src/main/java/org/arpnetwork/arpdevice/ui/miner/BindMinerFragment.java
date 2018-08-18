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
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.app.AtomicNonce;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.contracts.ARPContract;
import org.arpnetwork.arpdevice.contracts.ARPRegistry;
import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.contracts.tasks.BankAllowanceTask;
import org.arpnetwork.arpdevice.contracts.tasks.OnValueResult;
import org.arpnetwork.arpdevice.config.Constant;
import org.arpnetwork.arpdevice.dialog.PasswordDialog;
import org.arpnetwork.arpdevice.dialog.SeekBarDialog;
import org.arpnetwork.arpdevice.server.http.rpc.RPCRequest;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.bean.BindPromise;
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
import static org.arpnetwork.arpdevice.config.Constant.KEY_BINDPROMISE;
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASLIMIT;
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASPRICE;
import static org.arpnetwork.arpdevice.config.Constant.KEY_OP;
import static org.arpnetwork.arpdevice.config.Constant.KEY_PASSWD;

public class BindMinerFragment extends BaseFragment {
    private static final String TAG = "BindMinerFragment";

    public static final int OPERATION_ARP_APPROVE = 1;
    public static final int OPERATION_BANK_APPROVE = 2;
    public static final int OPERATION_BANK_DEPOSIT = 3;
    public static final int OPERATION_BIND = 4;
    public static final int OPERATION_UNBIND = 5;
    public static final int OPERATION_CANCEL_APPROVE = 6;

    private static final int LOCK_ARP = 500;
    private static final int SIGN_EXP = 4 * 60 * 60;

    private ListView mMinerList;
    private TextView mBandTipTextView;
    private MinerAdapter mAdapter;
    private View mApproveView;
    private Button mUnbindBtn;

    private TextView mApproveTextView;
    private OKHttpUtils mOkHttpUtils;

    private Miner mBoundMiner;

    private int mClickPosition;
    private BindPromise mBindPromise;
    private GasInfo mGasInfo;
    private String mGasTag = "gas_price";
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
        loadGasInfo();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mOkHttpUtils.cancelTag(mGasTag);

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
                if (StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING) != null
                        || StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING) != null) {
                    return;
                }
                if (!mAdapter.isBound(position)) {
                    mClickPosition = position;
                    if (mBoundMiner != null || StateHolder.getTaskByState(StateHolder.STATE_BIND_SUCCESS) != null) {
                        showMessageAlertDialog(null, getString(R.string.bind_change_bind_msg), getString(R.string.ok), null, null, null);
                    } else {
                        checkBalance(Util.getEthCost(mGasInfo.getGasPriceGwei(), mGasInfo.getGasLimit()).doubleValue());
                    }
                }
            }
        });

        mUnbindBtn = (Button) findViewById(R.id.btn_unbind);
        mUnbindBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*// TODO: 提示凭证兑换
                showMessageAlertDialog("兑换", "Message", "去兑换", "放弃兑换", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "去兑换");
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });*/
                // TODO: 兑换成功调用
                if (StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING) != null) {
                    return;
                }
                showPayEthDialog(getString(R.string.bind_unbind_title), getString(R.string.bind_unbind_msg), OPERATION_UNBIND);
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

        if (mBoundMiner != null) {
            showUnbindBtn(View.VISIBLE);
        } else {
            showUnbindBtn(View.GONE);
        }
    }

    private void showUnbindBtn(int visibility) {
        mUnbindBtn.setVisibility(visibility);
    }

    private void loadGasInfo() {
        showApproveView(R.string.loading);

        mOkHttpUtils.get(Config.API_URL, mGasTag, new SimpleCallback<GasInfoResponse>() {
            @Override
            public void onFailure(Request request, Exception e) {
                if (getActivity() != null) {
                    UIHelper.showToast(getActivity(), getString(R.string.load_gas_failed));
                    finish();
                }
            }

            @Override
            public void onSuccess(Response response, GasInfoResponse result) {
                mGasInfo = result.data;
                startLoad();
            }

            @Override
            public void onError(Response response, int code, Exception e) {
                if (getActivity() != null) {
                    UIHelper.showToast(getActivity(), getString(R.string.load_gas_failed));
                    finish();
                }
            }
        });
    }

    private void startLoad() {
        if (StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING) != null) {
            loadData();

            TaskInfo task = StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING);
            mAdapter.updateBindState(task.address, StateHolder.STATE_BIND_RUNNING);
        } else if (StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING) != null) {
            loadData();

            TaskInfo task = StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING);
            mAdapter.updateBindState(task.address, StateHolder.STATE_UNBIND_RUNNING);
        } else if (StateHolder.getTaskByState(StateHolder.STATE_APPROVE_RUNNING) != null
                || StateHolder.getTaskByState(StateHolder.STATE_BANK_APPROVE_RUNNING) != null
                || StateHolder.getTaskByState(StateHolder.STATE_DEPOSIT_RUNNING) != null) {
            showApproveView(R.string.bind_approving);
        } else {
            loadBindState();
        }
    }

    private void loadBindState() {
        String address = Wallet.get().getPublicKey();
        Miner miner = BindMinerHelper.getBound(address);
        if (miner != null) {
            mBoundMiner = miner;
            loadData();
        } else {
            loadBankAllowance();
        }
    }

    private void loadBankAllowance() {
        String owner = Wallet.get().getPublicKey();
        String spender = ARPRegistry.CONTRACT_ADDRESS;
        ARPBank.allowanceARP(owner, spender, new OnValueResult<BankAllowanceTask.BankAllowance>() {
            @Override
            public void onValueResult(BankAllowanceTask.BankAllowance result) {
                if (result != null && Convert.fromWei(result.amount.toString(), Convert.Unit.ETHER).doubleValue() >= LOCK_ARP) {
                    loadData();
                } else {
                    loadBankBalanceOf();
                }
            }
        });
    }

    private void loadBankBalanceOf() {
        String owner = Wallet.get().getPublicKey();
        ARPBank.balanceOf(owner, new OnValueResult<BigDecimal>() {
            @Override
            public void onValueResult(BigDecimal result) {
                if (result != null && result.intValue() >= LOCK_ARP) {
                    showPayEthDialog(getString(R.string.bind_bank_approve_title), getString(R.string.bind_bank_approve_msg), OPERATION_BANK_APPROVE);
                } else {
                    loadARPAllowance();
                }
            }
        });
    }

    private void loadARPAllowance() {
        String owner = Wallet.get().getPublicKey();
        String spender = ARPBank.CONTRACT_ADDRESS;
        ARPContract.allowanceARP(owner, spender, new OnValueResult<BigDecimal>() {
            @Override
            public void onValueResult(BigDecimal result) {
                if (result != null && result.intValue() >= LOCK_ARP) {
                    showPayEthDialog(getString(R.string.bind_bank_deposit_title), getString(R.string.bind_bank_deposit_msg), OPERATION_BANK_DEPOSIT);
                } else {
                    showPayEthDialog(getString(R.string.bind_arp_approve_title), getString(R.string.bind_arp_approve_msg), OPERATION_ARP_APPROVE);
                }
            }
        });
    }

    private void loadData() {
        showListView();

        List<Miner> miners = BindMinerHelper.getMinerList();
        for (int i = 0; i < miners.size(); i++) {
            Miner miner = miners.get(i);
//            String url = "http://" + miner.getIpString() + ":" + miner.getPortHttpInt();
            String url = "https://easy-mock.com/mock/5afea0ae6ba6060f61c231d1/example/load";
            loadMinerLoadInfo(i, url);
        }
        mAdapter.setData(miners);
        if (mBoundMiner != null) {
            mAdapter.updateBindState(mBoundMiner.address, StateHolder.STATE_BIND_SUCCESS);
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

    public void loadPromiseForBind(String url) {
        String nonce = AtomicNonce.getAndIncrement();

        RPCRequest request = new RPCRequest();
        request.setId(nonce);
        request.setMethod(Config.API_SERVER_VOUVHER);
        request.putString(Wallet.get().getPublicKey());
        request.putString(nonce);

        String json = request.toJSON();

        new OKHttpUtils().post(url, json, Config.API_SERVER_VOUVHER, new SimpleCallback<BindPromise>() {
            @Override
            public void onSuccess(Response response, BindPromise result) {
                String message = String.format(getString(R.string.bind_apply_message), result.getAmountHumanic());
                showAmountAlertDialog(null, message, result);
            }

            @Override
            public void onFailure(Request request, Exception e) {
                if (getActivity() != null) {
                    UIHelper.showToast(getActivity(), getString(R.string.load_promise_failed));
                }
            }

            @Override
            public void onError(Response response, int code, Exception e) {
                if (getActivity() != null) {
                    UIHelper.showToast(getActivity(), getString(R.string.load_promise_failed));
                }
            }
        });
    }

    private void checkBalance(final double ethCost) {
        // check balance before binding miner
        final String address = Wallet.get().getPublicKey();
        EtherAPI.getEtherBalance(address, new OnValueResult<BigDecimal>() {
            @Override
            public void onValueResult(BigDecimal result) {
                if (result.doubleValue() < ethCost) {
                    showErrorAlertDialog(null, getString(R.string.bind_miner_error_balance_insufficient));
                } else {
                    ARPContract.getArpBalance(address, new OnValueResult<BigDecimal>() {
                        @Override
                        public void onValueResult(BigDecimal result) {
                            if (result.doubleValue() < LOCK_ARP) {
                                showErrorAlertDialog(null, getString(R.string.bind_miner_error_balance_insufficient));
                            } else {
                                Miner miner = mAdapter.getItem(mClickPosition);
                                String url = "http://" + miner.getIpString() + ":" + miner.getPortHttpInt();
                                loadPromiseForBind(url);
                            }
                        }
                    });
                }
            }
        });
    }

    private void showAmountAlertDialog(String title, String message, final BindPromise bindPromise) {
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.bind_apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String owner = Wallet.get().getPublicKey();
                        String spender = mAdapter.getItem(mClickPosition).address;
                        ARPBank.allowanceARP(owner, spender, new OnValueResult<BankAllowanceTask.BankAllowance>() {
                            @Override
                            public void onValueResult(BankAllowanceTask.BankAllowance result) {
                                if (result != null && Convert.fromWei(result.amount.toString(), Convert.Unit.ETHER).doubleValue() >= LOCK_ARP) {
                                    // TODO: 提示旧凭证兑换 取消兑换状态判断？
                                    // 兑换后 解除授权，再执行绑定
                                    showPayEthDialog(getString(R.string.bind_cancel_approve_title), getString(R.string.bind_cancel_approve_msg), OPERATION_CANCEL_APPROVE);
                                } else {
                                    mBindPromise = bindPromise;
                                    Miner miner = mAdapter.getItem(mClickPosition);
                                    showPayEthDialog(miner.address, getString(R.string.bind_message), OPERATION_BIND);
                                }
                            }
                        });
                    }
                })
                .show();
    }

    private void showMessageAlertDialog(String title, String message,
            String positiveText, String negativeText, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener) {
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, positiveListener)
                .setNegativeButton(negativeText, negativeListener)
                .show();
    }

    private void showErrorAlertDialog(String title, String message) {
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(false)
                .show();
    }

    private void showPayEthDialog(String title, String message, final int opType) {
        if (mShowPriceDialog != null && mShowPriceDialog.isShowing()) return;

        final BigDecimal min = mGasInfo.getGasPriceGwei();
        final BigDecimal max = (mGasInfo.getGasPriceGwei().multiply(new BigDecimal("100")));
        BigDecimal defaultValue = mGasInfo.getGasPriceGwei();
        mGasPriceGWei = mGasInfo.getGasPriceGwei();

        String positiveText = getString(R.string.ok);
        switch (opType) {
            case OPERATION_ARP_APPROVE:
                positiveText = getString(R.string.bind_btn_arp_approve);
                break;

            case OPERATION_BANK_APPROVE:
                positiveText = getString(R.string.bind_btn_bank_approve);
                break;

            case OPERATION_BANK_DEPOSIT:
                positiveText = getString(R.string.bind_btn_deposit);
                break;

            case OPERATION_BIND:
                positiveText = getString(R.string.bind_btn_bind);
                break;

            case OPERATION_UNBIND:
                positiveText = getString(R.string.bind_btn_unbind);
                break;

            case OPERATION_CANCEL_APPROVE:
                positiveText = getString(R.string.bind_btn_cancel_approve);
                break;

            default:
                break;
        }

        final BigDecimal mEthSpend = Util.getEthCost(mGasInfo.getGasPriceGwei(), mGasInfo.getGasLimit());
        double yuan = Util.getYuanCost(defaultValue, mGasInfo.getGasLimit(), mGasInfo.getEthToYuanRate());

        final SeekBarDialog.Builder builder = new SeekBarDialog.Builder(getContext());
        builder.setTitle(title)
                .setMessage(message)
                .setSeekValue(0, String.format(getString(R.string.bind_eth_format), mEthSpend, yuan))
                .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            // min + progress * (max - min) / 100;
                            BigDecimal multiply = new BigDecimal(progress).multiply(max.subtract(min));
                            BigDecimal divide = multiply.divide(new BigDecimal("100"));
                            mGasPriceGWei = min.add(divide);
                            BigDecimal mEthSpend = Util.getEthCost(mGasPriceGWei, mGasInfo.getGasLimit());
                            double yuan = Util.getYuanCost(mGasPriceGWei, mGasInfo.getGasLimit(), mGasInfo.getEthToYuanRate());
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
                .setPositiveButton(positiveText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showInputPasswdDialog(opType);
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

                        if (opType == OPERATION_BIND) {
                            if (mBindPromise.getSignExpired().longValue() - System.currentTimeMillis() / 1000 > SIGN_EXP) { // 4小时内
                                showErrorAlertDialog(null, getString(R.string.bind_apply_expired));
                            } else {
                                startBindService(passwd);
                            }
                        } else if (opType == OPERATION_UNBIND) {
                            startUnbindService(passwd);
                        } else if (opType == OPERATION_CANCEL_APPROVE) {
                            startCancelApproveService(passwd);
                        } else {
                            startCommonService(passwd, opType);
                        }
                    }
                }
            }
        });
        mInputPasswdDialog = builder.create();
        mInputPasswdDialog.show();
    }

    private void startBindService(String passwd) {
        Intent mServiceIntent = getCommonIntent(passwd, OPERATION_BIND);
        mServiceIntent.putExtra(KEY_ADDRESS, mAdapter.getItem(mClickPosition).address);
        mServiceIntent.putExtra(KEY_BINDPROMISE, mBindPromise);
        getActivity().startService(mServiceIntent);
    }

    private void startUnbindService(String passwd) {
        Intent mServiceIntent = getCommonIntent(passwd, OPERATION_UNBIND);
        mServiceIntent.putExtra(KEY_ADDRESS, mBoundMiner.address);
        getActivity().startService(mServiceIntent);
    }

    private void startCancelApproveService(String passwd) {
        Intent mServiceIntent = getCommonIntent(passwd, OPERATION_CANCEL_APPROVE);
        mServiceIntent.putExtra(KEY_ADDRESS, mAdapter.getItem(mClickPosition).address);
        getActivity().startService(mServiceIntent);
    }

    private void startCommonService(String passwd, int opType) {
        getActivity().startService(getCommonIntent(passwd, opType));
    }

    private Intent getCommonIntent(String passwd, int opType) {
        Intent mServiceIntent = new Intent(getActivity(), BindMinerIntentService.class);
        mServiceIntent.putExtra(KEY_OP, opType);
        mServiceIntent.putExtra(KEY_PASSWD, passwd);
        mServiceIntent.putExtra(KEY_GASPRICE, getGasPrice().toString());
        mServiceIntent.putExtra(KEY_GASLIMIT, mGasInfo.getGasLimit().toString());
        return mServiceIntent;
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
                    showPayEthDialog(getString(R.string.bind_bank_deposit_title), getString(R.string.bind_bank_deposit_msg), OPERATION_BANK_DEPOSIT);
                    break;

                case StateHolder.STATE_APPROVE_FAILED:
                    showApproveView(R.string.bind_approve_failed);
                    break;

                case StateHolder.STATE_DEPOSIT_SUCCESS:
                    showPayEthDialog(getString(R.string.bind_bank_approve_title), getString(R.string.bind_bank_approve_msg), OPERATION_BANK_APPROVE);
                    break;

                case StateHolder.STATE_DEPOSIT_FAILED:
                    break;

                case StateHolder.STATE_BANK_APPROVE_SUCCESS:
                    loadData();

                    break;

                case StateHolder.STATE_BANK_APPROVE_FAILED:
                    break;

                case StateHolder.STATE_BIND_RUNNING:
                    TaskInfo task = StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING);
                    mAdapter.updateBindState(task.address, StateHolder.STATE_BIND_RUNNING);
                    break;

                case StateHolder.STATE_BIND_SUCCESS:
                    UIHelper.showToast(getActivity(), getString(R.string.bind_success));
                    showUnbindBtn(View.VISIBLE);

                    TaskInfo taskSuccess = StateHolder.getTaskByState(StateHolder.STATE_BIND_SUCCESS);
                    mAdapter.updateBindState(taskSuccess.address, StateHolder.STATE_BIND_SUCCESS);

                    mBoundMiner = new Miner();
                    mBoundMiner.address = taskSuccess.address;
                    break;

                case StateHolder.STATE_BIND_FAILED:
                    UIHelper.showToast(getActivity(), getString(R.string.bind_failed));

                    TaskInfo taskFailed = StateHolder.getTaskByState(StateHolder.STATE_BIND_FAILED);
                    mAdapter.updateBindState(taskFailed.address, StateHolder.STATE_BIND_FAILED);
                    break;

                case StateHolder.STATE_BANK_CANCEL_APPROVE_SUCCESS:
                    showPayEthDialog(mAdapter.getItem(mClickPosition).address, getString(R.string.bind_message), OPERATION_BIND);
                    break;

                case StateHolder.STATE_BANK_CANCEL_APPROVE_FAILED:
                    showApproveView(R.string.bind_approve_failed);
                    break;

                case StateHolder.STATE_UNBIND_RUNNING:
                    TaskInfo unbindTask = StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING);
                    mAdapter.updateBindState(unbindTask.address, StateHolder.STATE_UNBIND_RUNNING);
                    break;

                case StateHolder.STATE_UNBIND_SUCCESS:
                    UIHelper.showToast(getActivity(), getString(R.string.unbind_success));
                    showUnbindBtn(View.GONE);

                    TaskInfo unbindSuccess = StateHolder.getTaskByState(StateHolder.STATE_UNBIND_SUCCESS);
                    mAdapter.updateBindState(unbindSuccess.address, StateHolder.STATE_UNBIND_SUCCESS);

                    mBoundMiner = null;
                    StateHolder.clearAllState();

                    loadBindState();
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
