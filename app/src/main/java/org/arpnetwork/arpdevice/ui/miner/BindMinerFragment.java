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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.app.AtomicNonce;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.config.Constant;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.contracts.ARPRegistry;
import org.arpnetwork.arpdevice.contracts.api.VerifyAPI;
import org.arpnetwork.arpdevice.contracts.tasks.SimpleOnValueResult;
import org.arpnetwork.arpdevice.data.BankAllowance;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.dialog.PromiseDialog;
import org.arpnetwork.arpdevice.server.http.rpc.RPCRequest;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.bean.BindPromise;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.order.details.ExchangeActivity;
import org.arpnetwork.arpdevice.ui.widget.GasFeeView;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.util.OKHttpUtils;
import org.arpnetwork.arpdevice.util.SimpleCallback;
import org.arpnetwork.arpdevice.util.UIHelper;

import java.math.BigInteger;
import java.util.Locale;

import okhttp3.Request;
import okhttp3.Response;

import static org.arpnetwork.arpdevice.config.Constant.KEY_ADDRESS;
import static org.arpnetwork.arpdevice.config.Constant.KEY_BINDPROMISE;
import static org.arpnetwork.arpdevice.config.Constant.KEY_EXCHANGE_AMOUNT;
import static org.arpnetwork.arpdevice.config.Constant.KEY_EXCHANGE_TYPE;
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASLIMIT;
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASPRICE;
import static org.arpnetwork.arpdevice.config.Constant.KEY_OP;
import static org.arpnetwork.arpdevice.config.Constant.KEY_PASSWD;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerIntentService.OPERATION_BIND;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerIntentService.OPERATION_CASH;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerIntentService.OPERATION_UNBIND;

public class BindMinerFragment extends BaseFragment {
    private static final int SIGN_EXP = 3 * 60 * 60;

    private static final int BIND = OPERATION_BIND;
    private static final int UNBIND = OPERATION_UNBIND;

    private Miner mMiner;
    private BindPromise mBindPromise;
    private BigInteger mGasLimit;

    private LinearLayout mProgressView;
    private TextView mProgressTip;
    private TextView mAddressTextView;
    private TextView mTimeTextView;
    private TextView mAmountTextView;
    private LinearLayout mRemainingAmountLayout;
    private TextView mRemainingAmountTextView;
    private GasFeeView mGasView;
    private EditText mPasswordText;
    private Button mTaskBtn;

    private BindStateReceiver mBindStateReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.bind_miner);

        mMiner = (Miner) getArguments().getSerializable(Constant.KEY_MINER);

        registerReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bind_miner, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        initViews();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mGasView.cancelHttp();

        if (mBindStateReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBindStateReceiver);
            mBindStateReceiver = null;
        }
    }

    private void initViews() {
        mProgressView = (LinearLayout) findViewById(R.id.ll_progress);
        mProgressTip = (TextView) findViewById(R.id.tv_progress_tip);
        mAddressTextView = (TextView) findViewById(R.id.tv_address);
        mAmountTextView = (TextView) findViewById(R.id.tv_author_amount);
        mRemainingAmountLayout = (LinearLayout) findViewById(R.id.layout_remaining_amount);
        mRemainingAmountTextView = (TextView) findViewById(R.id.tv_remaining_amount);
        mTimeTextView = (TextView) findViewById(R.id.tv_author_time);
        mGasView = (GasFeeView) findViewById(R.id.ll_gas_fee);
        mPasswordText = (EditText) findViewById(R.id.et_password);
        mTaskBtn = (Button) findViewById(R.id.btn_task);

        mAddressTextView.setText(mMiner.getAddress());

        mGasView.setEthCallback(new GasFeeView.EthCallback() {
            @Override
            public void onEthNotEnough(boolean notEnough, BigInteger ethBalance) {
                if (notEnough) {
                    showErrorAlertDialog(R.string.register_underpaid, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                }
            }
        });

        BindMinerHelper.getBoundAsync(Wallet.get().getAddress(), new SimpleOnValueResult<Miner>() {
            @Override
            public void onValueResult(Miner result) {
                if (getActivity() == null) return;

                TaskInfo bindingTask = StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING);
                if (result != null && result.getAddress().equals(mMiner.getAddress())) {
                    TaskInfo unbindingTask = StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING);
                    if (bindingTask != null || (unbindingTask != null && unbindingTask.address.equals(mMiner.getAddress()))) {
                        setState(UNBIND);
                        setProgressState(UNBIND);
                    } else {
                        mMiner = result;
                        setState(UNBIND);
                    }
                } else {
                    if (bindingTask != null && bindingTask.address.equals(mMiner.getAddress())) {
                        setState(BIND);
                        setProgressState(BIND);
                    } else {
                        setState(BIND);
                    }
                }
            }

            @Override
            public void onFail(Throwable throwable) {
                if (getActivity() == null) return;

                mProgressView.setVisibility(View.GONE);
                showErrorAlertDialog(R.string.network_error, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
            }
        });
    }

    private void registerReceiver() {
        IntentFilter statusIntentFilter = new IntentFilter(
                Constant.BROADCAST_ACTION_STATUS);
        statusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        mBindStateReceiver = new BindStateReceiver();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mBindStateReceiver,
                statusIntentFilter);
    }

    private void setState(int state) {
        mProgressView.setVisibility(View.GONE);

        switch (state) {
            case BIND:
                setTitle(R.string.bind_miner);
                mRemainingAmountLayout.setVisibility(View.GONE);
                mTaskBtn.setBackgroundResource(R.drawable.btn_bg);
                mTaskBtn.setText(R.string.bind_btn_bind);

                String url = "http://" + mMiner.getIpString() + ":" + mMiner.getPortHttpInt();
                loadPromiseForBind(url);
                break;

            case UNBIND:
                setTitle(R.string.unbind_miner);
                mRemainingAmountLayout.setVisibility(View.VISIBLE);
                mTaskBtn.setBackgroundResource(R.drawable.btn_bg_orange);
                mTaskBtn.setText(R.string.bind_btn_unbind);
                mGasLimit = ARPRegistry.estimateUnbindGasLimit();
                mGasView.setGasLimit(mGasLimit);

                loadAllowance();
                break;
        }
    }

    private void setProgressState(int state) {
        mProgressView.setVisibility(View.VISIBLE);
        switch (state) {
            case BIND:
                mProgressTip.setText(R.string.binding);
                break;

            case UNBIND:
                mProgressTip.setText(R.string.unbinding);
                break;
        }
    }

    private void loadAllowance() {
        ARPBank.allowanceAsync(mMiner.getAddress(), Wallet.get().getAddress(), new SimpleOnValueResult<BankAllowance>() {
            @Override
            public void onValueResult(BankAllowance result) {
                if (getActivity() == null) return;

                if (result != null) {
                    float amount = result.getAmountHumanic().floatValue();
                    mAmountTextView.setText(String.format("%.2f ARP", amount));

                    float remainingAmount = amount;
                    Promise promise = Promise.get();
                    if (promise != null) {
                        remainingAmount = amount - promise.getFloatAmount();
                    }
                    mRemainingAmountTextView.setText(String.format("%.2f ARP", remainingAmount));

                    mTimeTextView.setText(mMiner.getExpired().compareTo(BigInteger.ZERO) == 0
                            || result.expired.compareTo(mMiner.getExpired()) < 0 ?
                            result.getExpiredHumanic(getContext()) : mMiner.getExpiredHumanic(getContext()));

                    mTaskBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String password = mPasswordText.getText().toString();
                            if (TextUtils.isEmpty(password) || Wallet.loadCredentials(password) == null) {
                                UIHelper.showToast(getActivity(), getString(R.string.input_passwd_error));
                            } else {
                                checkPromise();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFail(Throwable throwable) {
                if (getActivity() == null) return;

                showErrorAlertDialog(R.string.network_error, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
            }
        });
    }

    private void startBindService(String password) {
        Intent serviceIntent = getServiceIntent(password, OPERATION_BIND, mGasView.getGasPrice());
        serviceIntent.putExtra(KEY_BINDPROMISE, mBindPromise);
        getActivity().startService(serviceIntent);
    }

    private void startUnbindService(String password) {
        Intent serviceIntent = getServiceIntent(password, OPERATION_UNBIND, mGasView.getGasPrice());
        getActivity().startService(serviceIntent);
    }

    private Intent getServiceIntent(String password, int op, BigInteger gasWei) {
        Intent serviceIntent = new Intent(getActivity(), BindMinerIntentService.class);
        serviceIntent.putExtra(KEY_OP, op);
        serviceIntent.putExtra(KEY_PASSWD, password);
        serviceIntent.putExtra(KEY_ADDRESS, mMiner.getAddress());
        serviceIntent.putExtra(KEY_GASPRICE, gasWei.toString());
        serviceIntent.putExtra(KEY_GASLIMIT, mGasLimit.toString());
        return serviceIntent;
    }

    public void loadPromiseForBind(String url) {
        String nonce = AtomicNonce.getAndIncrement(mMiner.getAddress());

        RPCRequest request = new RPCRequest();
        request.setMethod(Config.API_SERVER_BIND_PROMISE);
        request.setId(nonce);
        request.putString(Wallet.get().getAddress());

        new OKHttpUtils().post(url, request.toJSON(), Config.API_SERVER_BIND_PROMISE, new SimpleCallback<BindPromise>() {
            @Override
            public void onSuccess(Response response, BindPromise result) {
                if (getActivity() == null) return;

                String data = String.format(Locale.US, "%s:%d:%d:%s:%s", result.getAmount(),
                        result.getSignExpired(), result.getExpired(), result.getPromiseSign(),
                        Wallet.get().getAddress());
                if (!VerifyAPI.verifySign(data, result.getSign(), mMiner.getAddress())) {
                    mAmountTextView.setText(String.format("%.2f ARP", result.getAmountHumanic().floatValue()));
                    mTimeTextView.setText(result.getExpiredHumanic(getContext()));
                    mBindPromise = result;

                    mGasLimit = ARPRegistry.estimateBindDeviceGasLimit(mMiner.getAddress(), mBindPromise);
                    mGasView.setGasLimit(mGasLimit);

                    mTaskBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mBindPromise.getSignExpired().longValue() - System.currentTimeMillis() / 1000 > SIGN_EXP) {
                                String password = mPasswordText.getText().toString();
                                if (Wallet.loadCredentials(password) != null) {
                                    startBindService(password);
                                    finish();
                                } else {
                                    UIHelper.showToast(getActivity(), getString(R.string.input_passwd_error));
                                }
                            } else {
                                showErrorAlertDialog(R.string.bind_apply_expired, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                });
                            }
                        }
                    });
                } else {
                    showErrorAlertDialog(R.string.load_promise_failed, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                }
            }

            @Override
            public void onFailure(Request request, Exception e) {
                if (getActivity() == null) return;

                showErrorAlertDialog(R.string.load_promise_failed, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
            }

            @Override
            public void onError(Response response, int code, Exception e) {
                if (getActivity() == null) return;

                showErrorAlertDialog(R.string.load_promise_failed, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
            }
        });
    }

    private void checkPromise() {
        PromiseDialog.show(getContext(), R.string.exchange_unbind_miner_msg,
                getString(R.string.exchange_unbind_miner_ignore), new PromiseDialog.PromiseListener() {
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
                        startActivity(ExchangeActivity.class, bundle);
                    }

                    @Override
                    public void onIgnore() {
                        if (getActivity() == null) return;

                        startUnbindService(mPasswordText.getText().toString());
                        finish();
                    }
                });
    }

    private class BindStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(Constant.EXTENDED_DATA_STATUS,
                    StateHolder.STATE_APPROVE_RUNNING)) {
                case StateHolder.STATE_BIND_RUNNING:
                    setProgressState(BIND);
                    break;

                case StateHolder.STATE_BIND_SUCCESS:
                    UIHelper.showToast(getActivity(), getString(R.string.bind_success));
                    TaskInfo bindTask = StateHolder.getTaskByState(StateHolder.STATE_BIND_SUCCESS);
                    if (bindTask != null && bindTask.address.equals(mMiner.getAddress())) {
                        setState(UNBIND);
                    } else {
                        setState(BIND);
                    }
                    break;

                case StateHolder.STATE_BIND_FAILED:
                    UIHelper.showToast(getActivity(), getString(R.string.bind_failed));

                    TaskInfo bindFailedTask = StateHolder.getTaskByState(StateHolder.STATE_BIND_FAILED);
                    if (bindFailedTask != null && bindFailedTask.address.equals(mMiner.getAddress())) {
                        setState(BIND);
                    } else {
                        setState(UNBIND);
                    }
                    break;

                case StateHolder.STATE_UNBIND_RUNNING:
                    setProgressState(UNBIND);
                    break;

                case StateHolder.STATE_UNBIND_SUCCESS:
                    UIHelper.showToast(getActivity(), getString(R.string.unbind_success));
                    finish();
                    break;

                case StateHolder.STATE_UNBIND_FAILED:
                    setState(UNBIND);
                    break;

                default:
                    break;
            }
        }
    }
}
