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

package org.arpnetwork.arpdevice.ui.unlock;

import android.content.BroadcastReceiver;
import android.content.Context;
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

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.config.Constant;
import org.arpnetwork.arpdevice.contracts.ARPRegistry;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.miner.BindMinerIntentService;
import org.arpnetwork.arpdevice.ui.miner.StateHolder;
import org.arpnetwork.arpdevice.ui.widget.GasFeeView;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.util.UIHelper;

import java.math.BigInteger;

import static org.arpnetwork.arpdevice.config.Constant.KEY_GASLIMIT;
import static org.arpnetwork.arpdevice.config.Constant.KEY_GASPRICE;
import static org.arpnetwork.arpdevice.config.Constant.KEY_OP;
import static org.arpnetwork.arpdevice.config.Constant.KEY_PASSWD;
import static org.arpnetwork.arpdevice.ui.miner.BindMinerIntentService.OPERATION_UNBIND;

public class UnlockFragment extends BaseFragment {
    private static BigInteger mGasLimit = BigInteger.ZERO;

    private LinearLayout mProgressView;
    private GasFeeView mGasView;
    private EditText mPasswordText;
    private Button mUnlockBtn;

    private BindStateReceiver mBindStateReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.unlock);
        registerReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_unlock, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        initViews();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mGasView.cancelHttp();
        unregisterReceiver();
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

    private void unregisterReceiver() {
        if (mBindStateReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBindStateReceiver);
            mBindStateReceiver = null;
        }
    }

    private void initViews() {
        mProgressView = (LinearLayout) findViewById(R.id.ll_progress);
        mPasswordText = (EditText) findViewById(R.id.et_password);

        mGasLimit = ARPRegistry.estimateUnbindGasLimit();
        mGasView = (GasFeeView) findViewById(R.id.ll_gas_fee);
        mGasView.setGasLimit(mGasLimit);

        mUnlockBtn = (Button) findViewById(R.id.btn_unlock);
        mUnlockBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCorrectPassword()) {
                    startServiceIntent(OPERATION_UNBIND);
                    hideSoftInput(mProgressView);
                } else {
                    UIHelper.showToast(getActivity(), getString(R.string.input_passwd_error));
                }
            }
        });
    }

    private void showProgress() {
        mProgressView.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mProgressView.setVisibility(View.GONE);
    }

    private boolean isCorrectPassword() {
        String password = mPasswordText.getText().toString();
        return !TextUtils.isEmpty(password) && Wallet.loadCredentials(password) != null;
    }

    private void startServiceIntent(int opType) {
        Intent serviceIntent = new Intent(getActivity(), BindMinerIntentService.class);
        serviceIntent.putExtra(KEY_OP, opType);
        serviceIntent.putExtra(KEY_PASSWD, mPasswordText.getText().toString());
        serviceIntent.putExtra(KEY_GASPRICE, mGasView.getGasPrice().toString());
        serviceIntent.putExtra(KEY_GASLIMIT, mGasLimit.toString());
        getActivity().startService(serviceIntent);
    }

    private class BindStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(Constant.EXTENDED_DATA_STATUS, StateHolder.STATE_UNBIND_RUNNING)) {
                case StateHolder.STATE_UNBIND_RUNNING:
                    showProgress();
                    break;

                case StateHolder.STATE_UNBIND_SUCCESS:
                    UIHelper.showToast(getActivity(), getString(R.string.unlock_success));
                    hideProgress();
                    finish();
                    break;

                case StateHolder.STATE_UNBIND_FAILED:
                    UIHelper.showToast(getActivity(), getString(R.string.unlock_failed));
                    hideProgress();
                    break;

                default:
                    break;
            }
        }
    }
}
