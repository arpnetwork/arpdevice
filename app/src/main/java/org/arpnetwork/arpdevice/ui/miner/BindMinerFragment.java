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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.contracts.ARPContract;
import org.arpnetwork.arpdevice.contracts.api.BalanceAPI;
import org.arpnetwork.arpdevice.contracts.tasks.OnValueResult;
import org.arpnetwork.arpdevice.config.Constant;
import org.arpnetwork.arpdevice.contracts.ARPRegistry;
import org.arpnetwork.arpdevice.contracts.tasks.BindMinerHelper;
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
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;

import okhttp3.Request;
import okhttp3.Response;

public class BindMinerFragment extends BaseFragment {
    private static final String TAG = "BindMinerFragment";

    private static final int LOCK_ARP = 500;

    private ListView mMinerList;
    private MinerAdapter mAdapter;
    private OKHttpUtils mOkHttpUtils;

    private int mClickPosition;
    private GasInfo mGasInfo;
    private BigDecimal mGasPriceGWei;
    private Dialog mShowPriceDialog;
    private Dialog mInputPasswdDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.bind_miner);

        mOkHttpUtils = new OKHttpUtils();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bind_miner, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
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
                    mClickPosition = position;
                    loadGasInfo();
                }
            }
        });
    }

    private void loadData() {
        List<Miner> miners = BindMinerHelper.getMinerList();
        for (int i = 0; i < miners.size(); i++) {
            Miner miner = miners.get(i);
            String url = "http://" + Util.longToIp(miner.ip.longValue()) + ":" + miner.port.intValue();
            Log.d(TAG, "miner url = " + url);
            loadMinerLoadInfo(i, url);
        }
        mAdapter.setData(miners);
        loadBindState();
    }

    private void loadBindState() {
        try {
            String address = Wallet.get().getPublicKey();
            Tuple3<String, BigInteger, BigInteger> binder = BindMinerHelper.devices(address);
            if (!TextUtils.isEmpty(binder.getValue1())) {
                mAdapter.updateBindState(binder.getValue1());
            }
        } catch (ExecutionException e) {
        } catch (InterruptedException e) {
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
                checkBalance(Util.getEthCost(result.data.getGasPriceGwei(), result.data.getGasLimit()).doubleValue());
            }

            @Override
            public void onError(Response response, int code, Exception e) {
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
        builder.setTitle(minerInfo.name)
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
                .setButton(getString(R.string.bind), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showInputPasswdDialog();
                    }
                });
        mShowPriceDialog = builder.create();
        mShowPriceDialog.show();
    }

    private void showInputPasswdDialog() {
        if (mInputPasswdDialog != null && mInputPasswdDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.input_passwd_tip);
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        View view = layoutInflater.inflate(R.layout.layout_input_passwd, null);
        builder.setView(view);

        final EditText edPasswd = (EditText) view.findViewById(R.id.ed_passwd);
        edPasswd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edPasswd.setFocusable(true);
                edPasswd.setFocusableInTouchMode(true);
                edPasswd.requestFocus();
                InputMethodManager inputManager = (InputMethodManager) edPasswd
                        .getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(edPasswd, 0);
            }
        });
        final Button btnOk = (Button) view.findViewById(R.id.btn_ok);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String passwd = edPasswd.getText().toString().trim();
                if (TextUtils.isEmpty(passwd)) {
                    UIHelper.showToast(getActivity(), getString(R.string.input_passwd_tip));
                } else {
                    final Credentials credentials = Wallet.loadCredentials(passwd);
                    if (credentials == null) {
                        UIHelper.showToast(getActivity(), getString(R.string.input_passwd_error));
                    } else {
                        mInputPasswdDialog.dismiss();
                        BindTask readTask = new BindTask(passwd);
                        readTask.execute(mAdapter.getItem(mClickPosition).address);


                    }
                }
            }
        });
        mInputPasswdDialog = builder.create();
        mInputPasswdDialog.show();
    }

    private BigInteger getGasPrice() {
        BigDecimal gas = Convert.toWei(mGasPriceGWei, Convert.Unit.GWEI);
        return gas.toBigInteger();
    }

    private boolean bindDevice(String address, Credentials credentials) {
        boolean success = false;
        ARPRegistry registry = ARPRegistry.load(BindMinerHelper.CONTRACT_ADDRESS, BalanceAPI.getWeb3J(),
                credentials, getGasPrice(), mGasInfo.getGasLimit());
        try {
            TransactionReceipt bindDeviceReceipt = registry.bindDevice(address).send();
            success = isStatusOK(bindDeviceReceipt.getStatus());
        } catch (Exception e) {
        }
        return success;
    }

    public boolean isStatusOK(String status) {
        if (null == status) {
            return true;
        }
        BigInteger statusQuantity = Numeric.decodeQuantity(status);
        return BigInteger.ONE.equals(statusQuantity);
    }

    private class BindTask extends AsyncTask<String, String, Boolean> {
        private Credentials credentials;
        private ProgressDialog progressDialog;

        public BindTask(String password) {
            credentials = Wallet.loadCredentials(password);
            progressDialog = new ProgressDialog(getActivity());
        }

        @Override
        protected void onPreExecute() {
            progressDialog.setMessage(getString(R.string.bind_handle));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            boolean result = false;
            final String address = params[0];

            String hexData = ARPContract.getTransactionHexData(BindMinerHelper.CONTRACT_ADDRESS,
                    credentials, getGasPrice(), new BigInteger("400000"));
            try {
                BalanceAPI.getWeb3J().ethSendRawTransaction(hexData).send();
                result = bindDevice(address, credentials);
            } catch (IOException e) {
                result = false;
            }

            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            if (result) {
                mAdapter.checkItem(mClickPosition);
                UIHelper.showToast(getActivity(), getString(R.string.bind_success));
            } else {
                UIHelper.showToast(getActivity(), getString(R.string.bind_failed));
            }
        }
    }
}