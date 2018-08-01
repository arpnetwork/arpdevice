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

import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.data.DeviceInfo;
import org.arpnetwork.arpdevice.device.DeviceManager;
import org.arpnetwork.arpdevice.dialog.SeekBarDialog;
import org.arpnetwork.arpdevice.server.DataServer;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.miner.BindMinerActivity;
import org.arpnetwork.arpdevice.ui.my.mywallet.MyWalletActivity;
import org.arpnetwork.arpdevice.ui.wallet.WalletManager;
import org.arpnetwork.arpdevice.util.NetworkHelper;
import org.arpnetwork.arpdevice.util.PreferenceManager;
import org.arpnetwork.arpdevice.util.UIHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyFragment extends BaseFragment implements View.OnClickListener {
    private static final String ORDER_PRICE = "order_price";

    private TextView mOrderPriceView;
    private int mQuality;
    private int mOrderPrice;

    private DeviceManager mDeviceManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.my);
        hideNavIcon();

        DataServer.getInstance().setListener(mConnectionListener);
        int orderPrice = PreferenceManager.getInstance().getInt(ORDER_PRICE);
        mOrderPrice = orderPrice >= 0 ? orderPrice : 1;
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

        if (mDeviceManager != null) {
            mDeviceManager.close();
        }
        getActivity().getApplication().onTerminate();
    }

    private void initViews() {
        TextView walletName = (TextView) findViewById(R.id.tv_wallet_name);
        walletName.setText(WalletManager.getInstance().getWallet().getName());

        mOrderPriceView = (TextView) findViewById(R.id.tv_order_price);
        mOrderPriceView.setText(String.format(getString(R.string.order_price_format), mOrderPrice));

        LinearLayout walletLayout = (LinearLayout) findViewById(R.id.layout_wallet);
        LinearLayout minerLayout = (LinearLayout) findViewById(R.id.layout_miner);
        LinearLayout priceLayout = (LinearLayout) findViewById(R.id.layout_order_price);
        LinearLayout detailsLayout = (LinearLayout) findViewById(R.id.layout_order_details);
        walletLayout.setOnClickListener(this);
        minerLayout.setOnClickListener(this);
        priceLayout.setOnClickListener(this);
        detailsLayout.setOnClickListener(this);

        Button btnOrder = (Button) findViewById(R.id.btn_order);
        btnOrder.setOnClickListener(this);

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
                info.connNetType = type;
                if (type == ConnectivityManager.TYPE_MOBILE) {
                    info.telNetType = NetworkHelper.getTelephonyNetworkType(getActivity().getApplicationContext());
                }

                startDeviceService();
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
            }

            @Override
            public void onDrawFrame(GL10 gl) {
            }
        });
    }

    private void startDeviceService() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeviceManager = new DeviceManager();
                mDeviceManager.setOnErrorListener(mOnErrorListener);
                mDeviceManager.connect();
                DataServer.getInstance().setDeviceManager(mDeviceManager);
            }
        });
    }

    private void startRecordIfNeeded() {
        if (Touch.getInstance().isRecording()) return;
        Touch.getInstance().startRecord(mQuality);
    }

    private void stopRecord() {
        Touch.getInstance().stopRecord();
    }

    private void showOrderPriceDialog() {
        final int min = 0;
        final int max = 100;
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
                            mOrderPrice = value;
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                })
                .setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mOrderPriceView.setText(String.format(getString(R.string.order_price_format), mOrderPrice));
                        PreferenceManager.getInstance().putInt(ORDER_PRICE, mOrderPrice);
                    }
                })
                .create()
                .show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layout_wallet:
                Intent intent = new Intent(getActivity(), MyWalletActivity.class);
                startActivity(intent);
                break;

            case R.id.layout_miner:
                Intent miner = new Intent(getActivity(), BindMinerActivity.class);
                startActivity(miner);
                break;

            case R.id.layout_order_price:
                showOrderPriceDialog();
                break;

            case R.id.layout_order_details:
                break;

            case R.id.btn_order:
                break;
        }
    }

    private DataServer.ConnectionListener mConnectionListener = new DataServer.ConnectionListener() {
        @Override
        public void onConnected() {
        }

        @Override
        public void onClosed() {
            stopRecord();
        }

        @Override
        public void onRecordStart(int quality) {
            mQuality = quality;
            startRecordIfNeeded();
        }

        @Override
        public void onRecordStop() {
            stopRecord();
        }

        @Override
        public void onException(Throwable cause) {
        }
    };

    private DeviceManager.OnErrorListener mOnErrorListener = new DeviceManager.OnErrorListener() {
        @Override
        public void onError(int code, final String msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    UIHelper.showToast(getContext(), msg, Toast.LENGTH_SHORT);
                }
            });
        }
    };
}
