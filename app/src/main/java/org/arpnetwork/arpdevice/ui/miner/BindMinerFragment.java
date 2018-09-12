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
import org.arpnetwork.arpdevice.data.BankAllowance;
import org.arpnetwork.arpdevice.server.http.rpc.RPCRequest;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.bean.BindPromise;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.view.GasFeeView;
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
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASLIMIT;
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASPRICE;
import static org.arpnetwork.arpdevice.config.Constant.KEY_OP;
import static org.arpnetwork.arpdevice.config.Constant.KEY_PASSWD;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerIntentService.OPERATION_BIND;
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
    private GasFeeView mGasFeeView;
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

        mGasFeeView.cancelHttp();

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
        mTimeTextView = (TextView) findViewById(R.id.tv_author_time);
        mGasFeeView = (GasFeeView) findViewById(R.id.ll_gas_fee);
        mPasswordText = (EditText) findViewById(R.id.et_password);
        mTaskBtn = (Button) findViewById(R.id.btn_task);

        mAddressTextView.setText(mMiner.getAddress());
        Miner boundMiner = BindMinerHelper.getBound(Wallet.get().getAddress());

        TaskInfo bindingTask = StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING);
        if (boundMiner != null && boundMiner.getAddress().equals(mMiner.getAddress())) {
            TaskInfo unbindingTask = StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING);
            if (bindingTask != null || (unbindingTask != null && unbindingTask.address.equals(mMiner.getAddress()))) {
                setState(UNBIND);
                setProgressState(UNBIND);
            } else {
                mMiner = boundMiner;
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
                String url = "http://" + mMiner.getIpString() + ":" + mMiner.getPortHttpInt();
                loadPromiseForBind(url);

                mTaskBtn.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                mTaskBtn.setText(R.string.bind_btn_bind);
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
                            showErrorAlertDialog(null, getString(R.string.bind_apply_expired));
                        }
                    }
                });
                break;

            case UNBIND:
                loadAllowance();
                mTaskBtn.setBackgroundColor(getResources().getColor(R.color.colorPrimaryRed));
                mTaskBtn.setText(R.string.bind_btn_unbind);
                mGasLimit = ARPRegistry.estimateUnbindGasLimit();
                mGasFeeView.setGasLimit(mGasLimit);
                mTaskBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String password = mPasswordText.getText().toString();
                        if (Wallet.loadCredentials(password) != null) {
                            startUnbindService(password);
                            finish();
                        } else {
                            UIHelper.showToast(getActivity(), getString(R.string.input_passwd_error));
                        }
                    }
                });
                break;
        }
    }

    private void showErrorAlertDialog(String title, String message) {
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
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
        BankAllowance allowance = ARPBank.allowance(mMiner.getAddress(), Wallet.get().getAddress());
        mAmountTextView.setText(String.format("%.2f", allowance.getAmountHumanic().floatValue()));

        mTimeTextView.setText(mMiner.getExpired().compareTo(BigInteger.ZERO) == 0
                || allowance.expired.compareTo(mMiner.getExpired()) < 0 ?
                allowance.getExpiredHumanic(getContext()): mMiner.getExpiredHumanic(getContext()));
    }

    private void startBindService(String password) {
        Intent serviceIntent = getServiceIntent(password, OPERATION_BIND, mGasFeeView.getGasPrice());
        serviceIntent.putExtra(KEY_BINDPROMISE, mBindPromise);
        getActivity().startService(serviceIntent);
    }

    private void startUnbindService(String password) {
        Intent serviceIntent = getServiceIntent(password, OPERATION_UNBIND, mGasFeeView.getGasPrice());
        getActivity().startService(serviceIntent);
    }

    private Intent getServiceIntent(String password, int op, BigInteger gasGWei) {
        Intent serviceIntent = new Intent(getActivity(), BindMinerIntentService.class);
        serviceIntent.putExtra(KEY_OP, op);
        serviceIntent.putExtra(KEY_PASSWD, password);
        serviceIntent.putExtra(KEY_ADDRESS, mMiner.getAddress());
        serviceIntent.putExtra(KEY_GASPRICE, gasGWei.toString());
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
                String data = String.format(Locale.US, "%s:%d:%d:%s:%s", result.getAmount(),
                        result.getSignExpired(), result.getExpired(), result.getPromiseSign(),
                        Wallet.get().getAddress());
                if (!VerifyAPI.verifySign(data, result.getSign(), mMiner.getAddress())) {
                    mAmountTextView.setText(String.format("%.2f", result.getAmountHumanic().floatValue()));
                    mTimeTextView.setText(result.getExpiredHumanic(getContext()));
                    mBindPromise = result;

                    mGasLimit = ARPRegistry.estimateBindDeviceGasLimit(mMiner.getAddress(), mBindPromise);
                    mGasFeeView.setGasLimit(mGasLimit);
                } else {
                    showErrorAlertDialog(null, getString(R.string.load_promise_failed));
                }
            }

            @Override
            public void onFailure(Request request, Exception e) {
                showErrorAlertDialog(null, getString(R.string.load_promise_failed));
            }

            @Override
            public void onError(Response response, int code, Exception e) {
                showErrorAlertDialog(null, getString(R.string.load_promise_failed));
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
