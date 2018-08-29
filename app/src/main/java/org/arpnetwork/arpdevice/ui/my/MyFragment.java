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

package org.arpnetwork.arpdevice.ui.my;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.opengl.GLSurfaceView;
import android.os.BatteryManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.config.Constant;
import org.arpnetwork.arpdevice.data.BankAllowance;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.miner.BindMinerHelper;
import org.arpnetwork.arpdevice.data.DeviceInfo;
import org.arpnetwork.arpdevice.dialog.PasswordDialog;
import org.arpnetwork.arpdevice.dialog.SeekBarDialog;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.miner.BindMinerActivity;
import org.arpnetwork.arpdevice.ui.miner.StateHolder;
import org.arpnetwork.arpdevice.ui.my.mywallet.MyWalletActivity;
import org.arpnetwork.arpdevice.ui.order.details.MyEarningActivity;
import org.arpnetwork.arpdevice.ui.order.receive.ReceiveOrderActivity;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.util.SignUtil;
import org.arpnetwork.arpdevice.util.NetworkHelper;
import org.arpnetwork.arpdevice.util.UIHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyFragment extends BaseFragment implements View.OnClickListener {
    private TextView mOrderPriceView;
    private int mOrderPrice;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.my);
        hideNavIcon();

        mOrderPrice = DeviceInfo.get().getPrice();
        CustomApplication.sInstance.startMonitorService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        CustomApplication.sInstance.stopMonitorService();
        getActivity().getApplication().onTerminate();
    }

    private void initViews() {
        TextView walletName = (TextView) findViewById(R.id.tv_wallet_name);
        walletName.setText(Wallet.get().getName());

        mOrderPriceView = (TextView) findViewById(R.id.tv_order_price);
        mOrderPriceView.setText(String.format(getString(R.string.order_price_format), mOrderPrice));

        findViewById(R.id.layout_wallet).setOnClickListener(this);
        findViewById(R.id.layout_miner).setOnClickListener(this);
        findViewById(R.id.layout_order_price).setOnClickListener(this);
        findViewById(R.id.layout_order_details).setOnClickListener(this);
        findViewById(R.id.btn_order).setOnClickListener(this);

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

    private void showOrderPriceDialog() {
        final int min = Config.ORDER_PRICE_LOW;
        final int max = Config.ORDER_PRICE_HIGH;
        int defaultValue = mOrderPrice;
        int value = (defaultValue - min) * 100 / (max - min);

        final SeekBarDialog.Builder builder = new SeekBarDialog.Builder(getContext());
        builder.setTitle(getString(R.string.order_price))
                .setMessage(getString(R.string.order_price_message))
                .setSeekValue(value, String.format(getString(R.string.order_price_format), defaultValue))
                .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            int value = min + progress * (max - min) / 100;
                            builder.setSeekValue(progress, String.format(getString(R.string.order_price_format), value));
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int value = min + builder.getProgress() * (max - min) / 100;
                        mOrderPrice = value;
                        mOrderPriceView.setText(String.format(getString(R.string.order_price_format), mOrderPrice));
                        DeviceInfo.get().setPrice(mOrderPrice);
                    }
                })
                .create()
                .show();
    }

    private void showNoBindingDialog() {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.no_bind_miner)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show();
    }

    private void showAlertDialog(int resId) {
        new AlertDialog.Builder(getContext())
                .setMessage(resId)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show();
    }

    private void showPasswordDialog(final Miner miner) {
        final PasswordDialog.Builder builder = new PasswordDialog.Builder(getContext());
        builder.setOnClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String password = builder.getPassword();
                if (TextUtils.isEmpty(password)) {
                    UIHelper.showToast(getActivity(), getString(R.string.input_passwd_tip));
                } else {
                    dialog.dismiss();
                    showProgressDialog("", false);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SignUtil.generateSigner(password);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hideProgressDialog();
                                    if (!SignUtil.signerExists()) {
                                        UIHelper.showToast(getActivity(), getString(R.string.input_passwd_error));
                                    } else {
                                        startReceivingOrder(miner);
                                    }
                                }
                            });
                        }
                    }).start();
                }
            }
        });
        builder.create().show();
    }

    private boolean isCharging() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getActivity().registerReceiver(null, intentFilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        return isCharging;
    }

    private void startReceivingOrder(Miner miner) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constant.KEY_MINER, miner);
        startActivity(ReceiveOrderActivity.class, bundle);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layout_wallet:
                startActivity(MyWalletActivity.class);
                break;

            case R.id.layout_miner:
                startActivity(BindMinerActivity.class);
                break;

            case R.id.layout_order_price:
                showOrderPriceDialog();
                break;

            case R.id.layout_order_details:
                startActivity(MyEarningActivity.class);
                break;

            case R.id.btn_order:
                if (!NetworkHelper.getInstance().isNetworkAvailable()) {
                    showAlertDialog(R.string.network_error);
                    return;
                }

                if (!isCharging()) {
                    UIHelper.showToast(getActivity(), getString(R.string.no_charging));
                } else {
                    Miner miner = BindMinerHelper.getBound(Wallet.get().getAddress());
                    if (miner != null) {
                        BankAllowance bankAllowance = BankAllowance.get();
                        if (bankAllowance == null) {
                            CustomApplication.sInstance.startMonitorService();
                            showAlertDialog(R.string.load_data_error);
                            return;
                        }

                        if (StateHolder.getTaskByState(StateHolder.STATE_UNBIND_RUNNING) != null) {
                            showAlertDialog(R.string.unbinding_miner);
                        } else if (!miner.expiredValid() || !bankAllowance.valid()) {
                            showAlertDialog(R.string.invalid_miner);
                        } else if (!SignUtil.signerExists()) {
                            showPasswordDialog(miner);
                        } else {
                            startReceivingOrder(miner);
                        }
                    } else {
                        showNoBindingDialog();
                    }
                }
                break;
        }
    }
}
