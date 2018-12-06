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

package org.arpnetwork.arpdevice.ui.home;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.opengl.GLSurfaceView;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.app.AtomicNonce;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.constant.Constant;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.contracts.ARPRegistry;
import org.arpnetwork.arpdevice.contracts.tasks.SimpleOnValueResult;
import org.arpnetwork.arpdevice.data.BankAllowance;
import org.arpnetwork.arpdevice.data.Promise;

import org.arpnetwork.arpdevice.data.Result;
import org.arpnetwork.arpdevice.rpc.RPCRequest;
import org.arpnetwork.arpdevice.ui.miner.MinerListActivity;
import org.arpnetwork.arpdevice.ui.miner.RegisterActivity;

import org.arpnetwork.arpdevice.data.DeviceInfo;
import org.arpnetwork.arpdevice.dialog.PasswordDialog;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.miner.BindMinerHelper;
import org.arpnetwork.arpdevice.ui.miner.StateHolder;
import org.arpnetwork.arpdevice.ui.miner.TaskInfo;
import org.arpnetwork.arpdevice.ui.mywallet.MyWalletActivity;
import org.arpnetwork.arpdevice.ui.order.details.MyEarningActivity;
import org.arpnetwork.arpdevice.ui.order.receive.ReceiveOrderActivity;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.util.OKHttpUtils;
import org.arpnetwork.arpdevice.util.SignUtil;
import org.arpnetwork.arpdevice.util.NetworkHelper;
import org.arpnetwork.arpdevice.util.SimpleCallback;
import org.arpnetwork.arpdevice.util.UIHelper;
import org.arpnetwork.arpdevice.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class HomeFragment extends BaseFragment implements View.OnClickListener {
    private static final String TAG = "HomeFragment";

    private static final int BIND_TYPE_COVER = 1;

    private TextView mOrderPriceView;
    private TextView mMinerName;
    private TextView mUnexchanged;
    private ImageView mArrow;
    private View mDivider;
    private LinearLayout mLayoutPriceSetting;
    private int mOrderPrice;
    private float mRemainingAmount;

    private PasswordDialog mPasswordDialog;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mOrderPrice = DeviceInfo.get().getPrice();

        CustomApplication.sInstance.startMonitorService();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.home);
        hideNavIcon();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews();
    }

    @Override
    public void onResume() {
        super.onResume();

        regBatteryChangedReceiver();
        loadMinerAddr();
        registerStateReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();

        unregBatteryChangedReceiver();
        dismissPasswordDialog();
        unregStateReceiver();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layout_wallet:
                startActivity(MyWalletActivity.class);
                break;

            case R.id.layout_miner:
                BindMinerHelper.getBoundAsync(Wallet.get().getAddress(), new SimpleOnValueResult<Miner>() {
                    @Override
                    public void onPreExecute() {
                        showProgressDialog("", false);
                    }

                    @Override
                    public void onValueResult(@Nullable Miner result) {
                        hideProgressDialog();

                        if (StateHolder.getTaskByState(StateHolder.STATE_BANK_CASH_RUNNING) != null) {
                            showAlertDialog(R.string.cashing);
                        } else if (StateHolder.getTaskByState(StateHolder.STATE_BANK_WITHDRAW_RUNNING) != null) {
                            showAlertDialog(R.string.withdrawing);
                        } else if (result == null) {
                            ARPBank.allowanceAsync(Wallet.get().getAddress(), ARPRegistry.CONTRACT_ADDRESS, new SimpleOnValueResult<BankAllowance>() {
                                @Override
                                public void onValueResult(BankAllowance result) {
                                    if (result == null || Convert.fromWei(result.amount.toString(),
                                            Convert.Unit.ETHER).doubleValue() < Double.valueOf(ARPBank.DEPOSIT_ARP_NUMBER)) {
                                        startActivity(RegisterActivity.class);
                                    } else {
                                        startActivity(MinerListActivity.class);
                                    }
                                }

                                @Override
                                public void onFail(Throwable throwable) {
                                    showErrorAlertDialog(R.string.network_error);
                                }
                            });
                        } else {
                            Bundle bundle = new Bundle();
                            bundle.putSerializable(Constant.KEY_MINER, result);
                            startActivity(MinerListActivity.class, bundle);
                        }
                    }

                    @Override
                    public void onFail(Throwable throwable) {
                        hideProgressDialog();
                        showErrorAlertDialog(R.string.network_error);
                    }
                });
                break;

            case R.id.layout_order_price:
                if (mLayoutPriceSetting.getVisibility() == View.GONE) {
                    mLayoutPriceSetting.setVisibility(View.VISIBLE);
                    mDivider.setVisibility(View.GONE);
                } else {
                    mLayoutPriceSetting.setVisibility(View.GONE);
                    mDivider.setVisibility(View.VISIBLE);
                }
                break;

            case R.id.layout_order_details:
                startActivity(MyEarningActivity.class);
                break;

            case R.id.btn_order:
                if (!NetworkHelper.getInstance().isWifiNetwork()) {
                    showAlertDialog(R.string.no_wifi);
                    return;
                }
                startReceiveOrder();
                break;
        }
    }

    private void initViews() {
        TextView walletName = (TextView) findViewById(R.id.tv_wallet_address);
        walletName.setText(Wallet.get().getAddress());

        mMinerName = (TextView) findViewById(R.id.tv_miner_name);

        mOrderPriceView = (TextView) findViewById(R.id.tv_order_price);
        mOrderPriceView.setText(String.format(getString(R.string.order_price_format), mOrderPrice));

        mArrow = (ImageView) findViewById(R.id.iv_arrow);
        mDivider = findViewById(R.id.divider);
        mLayoutPriceSetting = (LinearLayout) findViewById(R.id.layout_order_price_setting);

        int progress = (mOrderPrice - Config.ORDER_PRICE_LOW) * 100 / (Config.ORDER_PRICE_HIGH - Config.ORDER_PRICE_LOW);
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekbar);
        seekBar.setProgress(progress);
        seekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);

        mUnexchanged = (TextView) findViewById(R.id.tv_unexchanged);

        findViewById(R.id.layout_wallet).setOnClickListener(this);
        findViewById(R.id.layout_miner).setOnClickListener(this);
        findViewById(R.id.layout_order_price).setOnClickListener(this);
        findViewById(R.id.layout_order_details).setOnClickListener(this);
        findViewById(R.id.btn_order).setOnClickListener(this);

        TextView version = (TextView) findViewById(R.id.tv_version);
        version.setText(Util.getAppVersion(getActivity()));

        GLSurfaceView surfaceView = (GLSurfaceView) findViewById(R.id.gl_surface);
        surfaceView.setEGLContextClientVersion(1);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        surfaceView.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                String glRenderer = gl.glGetString(GL10.GL_RENDERER);

                DeviceInfo info = DeviceInfo.get();
                info.gpu = glRenderer;
                int type = NetworkHelper.getInstance().getNetworkType();
                info.connectivity = type;
                if (type == ConnectivityManager.TYPE_MOBILE) {
                    info.telephony = NetworkHelper.getTelephonyNetworkType(getActivity().getApplicationContext());
                }
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
            }

            @Override
            public void onDrawFrame(GL10 gl) {
            }
        });
    }

    private void setUnexchangedText(Miner miner) {
        if (miner != null) {
            String walletAddr = Wallet.get().getAddress();
            ARPBank.allowanceAsync(miner.getAddress(), walletAddr, new SimpleOnValueResult<BankAllowance>() {
                @Override
                public void onValueResult(BankAllowance allowance) {
                    if (allowance != null) {
                        BigInteger promiseAmount = BigInteger.ZERO;
                        final Promise promise = Promise.get();
                        if (promise != null) {
                            promiseAmount = promise.getAmountBig();

                            BigInteger unexchanged = promiseAmount.subtract(allowance.paid);
                            float fUnexchanged = Convert.fromWei(new BigDecimal(unexchanged), Convert.Unit.ETHER).floatValue();
                            if (fUnexchanged > 0) {
                                mUnexchanged.setText(String.format(getString(R.string.my_unexchanged), fUnexchanged));
                            } else {
                                mUnexchanged.setText("");
                            }
                        } else {
                            mUnexchanged.setText("");
                        }

                        mRemainingAmount = Convert.fromWei(new BigDecimal(allowance.amount.subtract(promiseAmount)), Convert.Unit.ETHER).floatValue();
                    }
                }

                @Override
                public void onFail(Throwable throwable) {
                }
            });
        } else {
            mUnexchanged.setText("");
        }
    }

    private void loadMinerAddr() {
        BindMinerHelper.getBoundAsync(Wallet.get().getAddress(), new SimpleOnValueResult<Miner>() {
            @Override
            public void onValueResult(Miner result) {
                if (result != null) {
                    mMinerName.setText(result.getAddress());
                } else {
                    mMinerName.setText("");
                }
                setUnexchangedText(result);
            }

            @Override
            public void onFail(Throwable throwable) {
                setUnexchangedText(null);
            }
        });
    }

    private void showNoBindingDialog() {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.no_bind_miner)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show();
    }

    private void showAlertDialog(int resId, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(getContext())
                .setMessage(resId)
                .setPositiveButton(R.string.ok, listener)
                .create()
                .show();
    }

    private void showAlertDialog(int resId) {
        showAlertDialog(resId, null);
    }

    private void showPasswordDialog(final Miner miner) {
        final PasswordDialog.Builder builder = new PasswordDialog.Builder(getContext());
        builder.setOnClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == PasswordDialog.CONFIRM) {
                    final String password = builder.getPassword();
                    if (TextUtils.isEmpty(password)) {
                        UIHelper.showToast(getActivity(), getString(R.string.input_passwd_tip));
                    } else {
                        dialog.dismiss();
                        bindDevice(miner, password);
                    }
                } else {
                    hideProgressDialog();
                }
            }
        });
        mPasswordDialog = builder.create();
        mPasswordDialog.show();
    }

    private void dismissPasswordDialog() {
        if (mPasswordDialog != null) {
            mPasswordDialog.dismiss();
            mPasswordDialog = null;
        }
    }

    private void regBatteryChangedReceiver() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        getActivity().registerReceiver(mBatteryChangedReceiver, intentFilter);
    }

    private void unregBatteryChangedReceiver() {
        getActivity().unregisterReceiver(mBatteryChangedReceiver);
    }

    private void registerStateReceiver() {
        IntentFilter statusIntentFilter = new IntentFilter(
                Constant.BROADCAST_ACTION_STATUS);
        statusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mBindStateReceiver,
                statusIntentFilter);
    }

    private void unregStateReceiver() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBindStateReceiver);
    }

    private void startReceiveOrderActivity(Miner miner) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constant.KEY_MINER, miner);

        Intent intent = new Intent(getActivity(), ReceiveOrderActivity.class);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void startReceiveOrder() {
        if (!NetworkHelper.getInstance().isWifiNetwork()) {
            showAlertDialog(R.string.no_wifi);
            return;
        }

        if (!Util.isCharging(getActivity())) {
            UIHelper.showToast(getActivity(), getString(R.string.no_charging));
        } else {
            BindMinerHelper.getBoundAsync(Wallet.get().getAddress(), new SimpleOnValueResult<Miner>() {
                @Override
                public void onPreExecute() {
                    showProgressDialog("", false);
                }

                @Override
                public void onValueResult(Miner result) {
                    if (result != null) {
                        BankAllowance bankAllowance = BankAllowance.get();
                        if (bankAllowance == null) {
                            CustomApplication.sInstance.startMonitorService();
                            hideProgressDialog();
                            showAlertDialog(R.string.load_data_error);
                            return;
                        }

                        if (StateHolder.getTaskByState(StateHolder.STATE_BIND_RUNNING) != null) {
                            showAlertDialog(R.string.binding_miner);
                        } else if (StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING) != null) {
                            showAlertDialog(R.string.unbinding_miner);
                        } else if (!result.expiredValid() || !bankAllowance.valid()) {
                            showAlertDialog(R.string.invalid_miner);
                        } else if (mRemainingAmount < Config.MIN_REMAINING_AMOUNT) {
                            showAlertDialog(R.string.amount_insufficient);
                        } else if (SignUtil.signerExists()) {
                            checkBind(result);
                            return;
                        } else {
                            showAlertDialog(R.string.account_not_exist);
                        }
                    } else {
                        showNoBindingDialog();
                    }
                    hideProgressDialog();
                }

                @Override
                public void onFail(Throwable throwable) {
                    hideProgressDialog();
                    showErrorAlertDialog(R.string.network_error);
                }
            });
        }
    }

    private void checkBind(final Miner miner) {
        RPCRequest request = new RPCRequest();
        request.setId(String.valueOf(new Random().nextInt(Integer.MAX_VALUE)));
        request.setMethod("device_checkBind");
        request.putString(Wallet.get().getAddress());
        request.putString(Wallet.getAccountAddress());

        String json = request.toJSON();
        String url = String.format(Locale.US, "http://%s:%d", miner.getIpString(), miner.getPortHttpInt());

        new OKHttpUtils().post(url, json, "checkBind", new SimpleCallback<Void>() {
            @Override
            public void onSuccess(okhttp3.Response response, Void result) {
                hideProgressDialog();
                startReceiveOrderActivity(miner);
            }

            @Override
            public void onFailure(okhttp3.Request request, Exception e) {
                hideProgressDialog();
                UIHelper.showToast(getContext(), R.string.network_error);
            }

            @Override
            public void onError(okhttp3.Response response, int code, Exception e) {
                getNonce(miner);
            }
        });
    }

    private void getNonce(final Miner miner) {
        RPCRequest request = new RPCRequest();
        request.setId(String.valueOf(new Random().nextInt(Integer.MAX_VALUE)));
        request.setMethod("nonce_get");
        request.putString(Wallet.get().getAddress());

        String json = request.toJSON();
        String url = String.format(Locale.US, "http://%s:%d", miner.getIpString(), miner.getPortHttpInt());

        new OKHttpUtils().post(url, json, "getNonce", new SimpleCallback<Result>() {
            @Override
            public void onSuccess(okhttp3.Response response, Result result) {
                AtomicNonce.sync(result.getNonce(), miner.getAddress());
                showPasswordDialog(miner);
            }

            @Override
            public void onFailure(okhttp3.Request request, Exception e) {
                hideProgressDialog();
                UIHelper.showToast(getContext(), R.string.network_error);
            }

            @Override
            public void onError(okhttp3.Response response, int code, Exception e) {
                hideProgressDialog();
                UIHelper.showToast(getContext(), R.string.start_ordering_failed);
            }
        });
    }

    private void bindDevice(final Miner miner, String password) {
        int type = BIND_TYPE_COVER;
        String walletAddr = Wallet.get().getAddress();
        String nonce = AtomicNonce.getAndIncrement(miner.getAddress());
        String subAddrList = "";
        try {
            String salt = Util.getRandomString(32);

            JSONObject object = new JSONObject();
            object.put("sub_addr", Wallet.getAccountAddress());
            object.put("salt", salt);
            String sign = SignUtil.signWithAccount(salt);
            object.put("sub_sign", sign);

            JSONArray array = new JSONArray();
            array.put(object);

            subAddrList = array.toString();
        } catch (JSONException e) {
        }

        RPCRequest request = new RPCRequest();
        request.setId(String.valueOf(new Random().nextInt(Integer.MAX_VALUE)));
        request.setMethod("device_bind");
        request.putString(walletAddr);
        request.putInt(type);
        request.putString(subAddrList);
        request.putString(nonce);

        String data = String.format(Locale.US, "%s:%s:%d:%s:%s:%s", "device_bind", walletAddr, type, subAddrList, nonce, miner.getAddress());

        String sign = SignUtil.signWithWallet(password, data);
        request.putString(sign);

        String json = request.toJSON();
        String url = String.format(Locale.US, "http://%s:%d", miner.getIpString(), miner.getPortHttpInt());

        new OKHttpUtils().post(url, json, "bindDevice", new SimpleCallback<Void>() {
            @Override
            public void onSuccess(okhttp3.Response response, Void result) {
                hideProgressDialog();
                startReceiveOrderActivity(miner);
            }

            @Override
            public void onFailure(okhttp3.Request request, Exception e) {
                hideProgressDialog();
                UIHelper.showToast(getContext(), R.string.network_error);
            }

            @Override
            public void onError(okhttp3.Response response, int code, Exception e) {
                hideProgressDialog();
                UIHelper.showToast(getContext(), R.string.start_ordering_failed);
            }
        });
    }

    private BroadcastReceiver mBatteryChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;
                if (!isCharging) {
                    dismissPasswordDialog();
                }
            }
        }
    };

    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                int value = Config.ORDER_PRICE_LOW + progress * (Config.ORDER_PRICE_HIGH - Config.ORDER_PRICE_LOW) / 100;
                mOrderPriceView.setText(String.format(getString(R.string.order_price_format), value));
                mOrderPrice = value;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            DeviceInfo.get().setPrice(mOrderPrice);
        }
    };

    private BroadcastReceiver mBindStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(Constant.EXTENDED_DATA_STATUS,
                    StateHolder.STATE_APPROVE_RUNNING)) {
                case StateHolder.STATE_BIND_SUCCESS:
                    UIHelper.showToast(getActivity(), getString(R.string.bind_success));
                    TaskInfo bindTask = StateHolder.getTaskByState(StateHolder.STATE_BIND_SUCCESS);
                    mMinerName.setText(bindTask.address);
                    break;

                case StateHolder.STATE_UNBIND_SUCCESS:
                    UIHelper.showToast(getActivity(), getString(R.string.unbind_success));
                    mMinerName.setText("");
                    break;

                default:
                    break;
            }
        }
    };
}
