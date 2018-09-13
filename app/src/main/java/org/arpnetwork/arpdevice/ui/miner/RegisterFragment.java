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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.config.Constant;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.contracts.ARPContract;
import org.arpnetwork.arpdevice.contracts.ARPRegistry;
import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.contracts.tasks.OnValueResult;
import org.arpnetwork.arpdevice.data.BankAllowance;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.view.GasFeeView;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.util.UIHelper;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.arpnetwork.arpdevice.config.Constant.KEY_GASLIMIT;
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASPRICE;
import static org.arpnetwork.arpdevice.config.Constant.KEY_OP;
import static org.arpnetwork.arpdevice.config.Constant.KEY_PASSWD;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerIntentService.OPERATION_ARP_APPROVE;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerIntentService.OPERATION_BANK_APPROVE;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerIntentService.OPERATION_BANK_DEPOSIT;

public class RegisterFragment extends BaseFragment {

    private static final int APPROVE = OPERATION_ARP_APPROVE;
    private static final int DEPOSIT = OPERATION_BANK_DEPOSIT;
    private static final int LOCK_UP = OPERATION_BANK_APPROVE;

    private LinearLayout mProgressView;
    private TextView mProgressTip;
    private TextView mStepText;
    private TextView mStepTipText;
    private LinearLayout mAmountView;
    private TextView mAmountText;
    private TextView mAmountValue;
    private LinearLayout mValueView;
    private GasFeeView mGasView;
    private EditText mPasswordText;
    private Button mForwardBtn;
    private static ProgressDialog mProgressDialog;

    private static BigInteger mGasLimit = BigInteger.ZERO;
    private static int mStep;

    private boolean alertShown = false;

    private BindStateReceiver mBindStateReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.register_device);

        registerReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
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
        mStepText = (TextView) findViewById(R.id.tv_step_indicate);
        mStepTipText = (TextView) findViewById(R.id.tv_step_tip);
        mAmountView = (LinearLayout) findViewById(R.id.ll_amount);
        mAmountText = (TextView) findViewById(R.id.tv_amount_text);
        mAmountValue = (TextView) findViewById(R.id.tv_amount);
        mValueView = (LinearLayout) findViewById(R.id.ll_cost);
        mGasView = (GasFeeView) findViewById(R.id.ll_gas_fee);
        mPasswordText = (EditText) findViewById(R.id.et_password);
        mForwardBtn = (Button) findViewById(R.id.btn_forward);
        mForwardBtn.setEnabled(false);

        mForwardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = mPasswordText.getText().toString();
                if (TextUtils.isEmpty(password) || Wallet.loadCredentials(password) == null) {
                    UIHelper.showToast(getActivity(), R.string.input_passwd_error);
                    return;
                }

                final String address = Wallet.get().getAddress();
                EtherAPI.getEtherBalance(address, new OnValueResult<BigInteger>() {
                    @Override
                    public void onValueResult(BigInteger result) {
                        if (result == null) {
                            showErrorAlertDialog(getString(R.string.network_error));
                        } else if (result.compareTo(mGasView.getGasCost()) < 0) {
                            UIHelper.showToast(getActivity(), R.string.register_underpaid);
                        } else {
                            startServiceIntent(mStep);
                        }
                    }
                });
            }
        });
        showProgressBar(getActivity(), null, false);
        startLoad();
    }

    private void setStepState(int stepState) {
        hideProgressBar();
        findViewById(R.id.ll_register).setVisibility(View.VISIBLE);
        mProgressView.setVisibility(View.GONE);
        mPasswordText.setText("");

        String deviceAddress = Wallet.get().getAddress();
        String allSteps = getString(R.string.register_author) + getString(R.string.register_transfer) + getString(R.string.register_lock);

        switch (stepState) {
            case APPROVE:
                mStepText.setText(highlight(allSteps, getString(R.string.register_author)));
                mStepTipText.setText(getString(R.string.register_author_tip));
                mAmountView.setVisibility(View.GONE);
                mValueView.setVisibility(View.GONE);

                mGasLimit = ARPContract.estimateApproveGasLimit();
                mGasView.setGasLimit(mGasLimit);

                mForwardBtn.setText(R.string.register_next_step);
                break;

            case DEPOSIT:
                mStepText.setText(highlight(allSteps, getString(R.string.register_author) + getString(R.string.register_transfer)));
                mStepTipText.setText(getString(R.string.register_transfer_tip));
                mAmountView.setVisibility(View.VISIBLE);
                mAmountText.setText(R.string.register_transfer_amount);
                float totalAmount = Convert.fromWei(new BigDecimal(ARPContract.balanceOf(deviceAddress)), Convert.Unit.ETHER).floatValue();
                mAmountValue.setText(String.format(getString(R.string.register_total_amount), totalAmount));
                mValueView.setVisibility(View.VISIBLE);

                mGasLimit = ARPBank.estimateDepositGasLimit();
                mGasView.setGasLimit(mGasLimit);

                mForwardBtn.setText(R.string.register_next_step);
                if (totalAmount < Float.parseFloat(ARPBank.DEPOSIT_ARP_NUMBER)) {
                    mForwardBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            UIHelper.showToast(getActivity(), R.string.register_insufficient_balance);
                        }
                    });
                }
                break;

            case LOCK_UP:
                mStepText.setText(highlight(allSteps, allSteps));
                mStepTipText.setText(getString(R.string.register_lock_tip));
                mAmountView.setVisibility(View.VISIBLE);
                mAmountText.setText(R.string.register_lock_amount);
                mAmountValue.setText(String.format(getString(R.string.register_token_amount), ARPBank.APPROVE_ARP_NUMBER));
                mValueView.setVisibility(View.GONE);

                mGasLimit = ARPBank.estimateApproveGasLimit(deviceAddress);
                mGasView.setGasLimit(mGasLimit);

                mForwardBtn.setText(R.string.register_finished);
                break;

            default:
                break;
        }

        mStep = stepState;
        mForwardBtn.setEnabled(true);
    }

    private void startServiceIntent(int opType) {
        Intent serviceIntent = new Intent(getActivity(), BindMinerIntentService.class);
        serviceIntent.putExtra(KEY_OP, opType);
        serviceIntent.putExtra(KEY_PASSWD, mPasswordText.getText().toString());
        serviceIntent.putExtra(KEY_GASPRICE, mGasView.getGasPrice().toString());
        serviceIntent.putExtra(KEY_GASLIMIT, mGasLimit.toString());
        getActivity().startService(serviceIntent);
    }

    private SpannableStringBuilder highlight(String allText, String highlight) {
        SpannableStringBuilder style = new SpannableStringBuilder(allText);
        int startIndex = style.toString().indexOf(highlight);
        if (startIndex >= 0) {
            style.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)),
                    startIndex, startIndex + highlight.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        }
        return style;
    }

    private void setProcess(int stepState) {
        mProgressView.setVisibility(View.VISIBLE);
        switch (stepState) {
            case APPROVE:
                mProgressTip.setText(R.string.register_authoring);
                break;

            case DEPOSIT:
                mProgressTip.setText(R.string.register_transferring);
                break;

            case LOCK_UP:
                mProgressTip.setText(R.string.register_locking);
                break;

            default:
                break;
        }
    }

    private void startLoad() {
        if (StateHolder.getTaskByState(StateHolder.STATE_APPROVE_RUNNING) != null) {
            setStepState(APPROVE);
            setProcess(APPROVE);
        } else if (StateHolder.getTaskByState(StateHolder.STATE_DEPOSIT_RUNNING) != null) {
            setStepState(DEPOSIT);
            setProcess(DEPOSIT);
        } else if (StateHolder.getTaskByState(StateHolder.STATE_BANK_APPROVE_RUNNING) != null) {
            setStepState(LOCK_UP);
            setProcess(LOCK_UP);
        } else {
            loadBindState();
        }
    }

    private void loadBindState() {
        String address = Wallet.get().getAddress();
        Miner miner = BindMinerHelper.getBound(address);
        if (miner != null) {
            showMinerList();
        } else {
            loadBankAllowance();
        }
    }

    private void loadBankAllowance() {
        String owner = Wallet.get().getAddress();
        String spender = ARPRegistry.CONTRACT_ADDRESS;
        BankAllowance allowance = ARPBank.allowance(owner, spender);

        if (allowance != null && Convert.fromWei(allowance.amount.toString(),
                Convert.Unit.ETHER).doubleValue() >= Double.valueOf(ARPBank.DEPOSIT_ARP_NUMBER)) {
            showMinerList();
        } else {
            loadBankBalanceOf();
        }
    }

    private void loadBankBalanceOf() {
        String owner = Wallet.get().getAddress();
        BigInteger balance = ARPBank.balanceOf(owner);
        int intValue = Convert.fromWei(balance.toString(), Convert.Unit.ETHER).intValue();

        if (balance != null && intValue >= Double.valueOf(ARPBank.DEPOSIT_ARP_NUMBER)) {
            setStepState(LOCK_UP);
        } else {
            loadARPAllowance();
        }
    }

    private void loadARPAllowance() {
        String owner = Wallet.get().getAddress();
        String spender = ARPBank.CONTRACT_ADDRESS;
        BigInteger allowance = ARPContract.allowance(owner, spender);

        if (allowance != null && Convert.fromWei(allowance.toString(), Convert.Unit.ETHER).doubleValue() >= Double.valueOf(ARPBank.DEPOSIT_ARP_NUMBER)) {
            setStepState(DEPOSIT);
        } else {
            setStepState(APPROVE);
        }
    }

    private void registerReceiver() {
        IntentFilter statusIntentFilter = new IntentFilter(
                Constant.BROADCAST_ACTION_STATUS);
        statusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        mBindStateReceiver = new BindStateReceiver();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mBindStateReceiver, statusIntentFilter);
    }

    private class BindStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(Constant.EXTENDED_DATA_STATUS,
                    StateHolder.STATE_APPROVE_RUNNING)) {
                case StateHolder.STATE_APPROVE_RUNNING:
                    setProcess(APPROVE);
                    break;

                case StateHolder.STATE_APPROVE_SUCCESS:
                    UIHelper.showToast(getActivity(), getString(R.string.bind_approve_success));
                    setStepState(DEPOSIT);
                    break;

                case StateHolder.STATE_APPROVE_FAILED:
                    UIHelper.showToast(getActivity(), getString(R.string.bind_approve_failed));
                    break;

                case StateHolder.STATE_DEPOSIT_RUNNING:
                    setProcess(DEPOSIT);
                    break;

                case StateHolder.STATE_DEPOSIT_SUCCESS:
                    UIHelper.showToast(getActivity(), getString(R.string.bind_deposit_success));
                    setStepState(LOCK_UP);
                    break;

                case StateHolder.STATE_DEPOSIT_FAILED:
                    UIHelper.showToast(getActivity(), getString(R.string.bind_deposit_failed));
                    break;

                case StateHolder.STATE_BANK_APPROVE_RUNNING:
                    setProcess(LOCK_UP);
                    break;

                case StateHolder.STATE_BANK_APPROVE_SUCCESS:
                    showMinerList();
                    break;

                case StateHolder.STATE_BANK_APPROVE_FAILED:
                    UIHelper.showToast(getActivity(), getString(R.string.bind_lock_failed));
                    break;

                default:
                    break;
            }
        }
    }

    private void showMinerList() {
        startActivity(MinerListActivity.class);
        finish();
    }

    private static void showProgressBar(final Activity context, String msg, boolean cancel) {
        mProgressDialog = ProgressDialog.show(context, null, msg);
        mProgressDialog.setCanceledOnTouchOutside(cancel);
        mProgressDialog.setCancelable(cancel);
    }

    private static void hideProgressBar() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    private void showErrorAlertDialog(String message) {
        if (!alertShown && getContext() != null) {
            alertShown = true;
            new AlertDialog.Builder(getContext())
                    .setMessage(message)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            alertShown = false;
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }
}
