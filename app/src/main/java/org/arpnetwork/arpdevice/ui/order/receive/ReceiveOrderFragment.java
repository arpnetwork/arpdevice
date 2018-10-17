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

package org.arpnetwork.arpdevice.ui.order.receive;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import org.arpnetwork.arpdevice.constant.ErrorCode;
import org.arpnetwork.arpdevice.data.DeviceInfo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.app.AppManager;
import org.arpnetwork.arpdevice.app.DAppApi;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.constant.Constant;
import org.arpnetwork.arpdevice.data.BankAllowance;
import org.arpnetwork.arpdevice.data.DApp;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.device.DeviceManager;
import org.arpnetwork.arpdevice.device.TaskHelper;
import org.arpnetwork.arpdevice.download.DownloadManager;
import org.arpnetwork.arpdevice.proxy.HttpProxy;
import org.arpnetwork.arpdevice.proxy.PortProxy;
import org.arpnetwork.arpdevice.proxy.TcpProxy;
import org.arpnetwork.arpdevice.server.DataServer;
import org.arpnetwork.arpdevice.server.http.Dispatcher;
import org.arpnetwork.arpdevice.server.http.HttpServer;
import org.arpnetwork.arpdevice.server.http.rpc.RPCRequest;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.ui.CheckDeviceActivity;
import org.arpnetwork.arpdevice.ui.base.BaseActivity;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.util.NetworkHelper;
import org.arpnetwork.arpdevice.util.OKHttpUtils;
import org.arpnetwork.arpdevice.util.SimpleCallback;
import org.arpnetwork.arpdevice.util.Util;

import java.math.BigInteger;
import java.util.Locale;

public class ReceiveOrderFragment extends BaseFragment implements PromiseHandler.OnReceivePromiseListener,
        TaskHelper.OnTopTaskListener, AppManager.OnAppManagerListener {
    private static final String TAG = ReceiveOrderFragment.class.getSimpleName();

    private TextView mOrderStateView;
    private View mFloatView;

    private DeviceManager mDeviceManager;
    private HttpServer mHttpServer;
    private AppManager mAppManager;
    private Miner mMiner;

    private DefaultRPCDispatcher mDispatcher;
    private PortProxy mTcpPortProxy;
    private PortProxy mHttpPortProxy;
    private TcpProxy mTcpProxy;
    private HttpProxy mHttpProxy;

    private BigInteger mLastAmount = BigInteger.ZERO;
    private BigInteger mReceivedAmount = BigInteger.ZERO;
    private long mLaunchTime;
    private int mQuality;
    private int mTotalTime;
    private boolean mStartService;
    private boolean mPaused;
    private boolean mCheckInstall;
    private boolean mInstallSuccess;

    private TouchLocalReceiver mTouchLocalReceiver;
    private ChargingReceiver mChargingReceiver;
    private MinerStateChangedReceiver mStateChangedReceiver;

    private Dialog mAlertDialog;
    private Handler mHandler = new Handler();

    private int mDataPort;
    private int mHttpPort;

    private float mDeviceBright = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.receive_order);
        getBaseActivity().setOnBackListener(mOnBackListener);

        mMiner = (Miner) getArguments().getSerializable(Constant.KEY_MINER);

        int[] ports = CustomApplication.sInstance.getPortArray();
        mDataPort = ports[0];
        mHttpPort = ports[1];

        registerReceiver();
        NetworkHelper.getInstance().registerNetworkListener(mNetworkChangeListener);

        mAppManager = AppManager.getInstance(getContext().getApplicationContext());
        mDeviceBright = Util.getScreenBrightness(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_receive_order, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews();
    }

    @Override
    public void onResume() {
        super.onResume();

        mHandler.removeCallbacks(mStopRunnable);
        if (!mPaused || mCheckInstall) {
            startDeviceService();
            mCheckInstall = false;
            mPaused = false;
        } else {
            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mAppManager.getState() != AppManager.State.INSTALLING && mAppManager.getState() != AppManager.State.LAUNCHING) {
            mHandler.postDelayed(mStopRunnable, 500);
        }
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        NetworkHelper.getInstance().unregisterNetworkListener(mNetworkChangeListener);
        stopDeviceService();
        unregisterReceiver();
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }
        Util.dimOff(getActivity(), mDeviceBright);

        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            mInstallSuccess = true;
        }
    }

    @Override
    public void onTopTaskIllegal(String pkgName) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                stopDeviceService();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkTopTaskAndStart();
                    }
                }, 2000);
            }
        });

        long now = System.currentTimeMillis();
        if (now - mLaunchTime < 2000) {
            DAppApi.appStop(pkgName, 0, mDeviceManager.getDapp());
        }
    }

    @Override
    public void onAppInstall(boolean success) {
        if (!success && !mInstallSuccess) {
            startCheckActivity(mMiner);
        } else {
            mInstallSuccess = true;
        }
    }

    @Override
    public void onAppLaunch(final boolean success) {
        mLaunchTime = success ? System.currentTimeMillis() : 0;
        DataServer.getInstance().onAppLaunch(success);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (success) {
                    showFloatLayer();
                }
            }
        });
    }

    private void initViews() {
        mOrderStateView = (TextView) findViewById(R.id.tv_order_state);
        mOrderStateView.setText(R.string.starting_service);
        mFloatView = findViewById(R.id.view_float);

        Button exitButton = (Button) findViewById(R.id.btn_exit);
        exitButton.setOnClickListener(mOnClickExitListener);
    }

    private void showFloatLayer() {
        ((BaseActivity) getActivity()).hideToolbar();
        mFloatView.setVisibility(View.VISIBLE);
    }

    private void hideFloatLayer() {
        if (getActivity() != null) {
            ((BaseActivity) getActivity()).showToolbar();
            mFloatView.setVisibility(View.GONE);
        }
    }

    private void checkTopTaskAndStart() {
        mAppManager.getTopTask(new TaskHelper.OnGetTopTaskListener() {
            @Override
            public void onGetTopTask(String pkgName) {
                if (pkgName.contains(getContext().getPackageName())) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            startDeviceService();
                        }
                    });
                }
            }
        });
    }

    private void startCheckActivity(Miner miner) {
        if (!mCheckInstall) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(Constant.KEY_MINER, miner);
            bundle.putBoolean(Constant.KEY_FROM_MY, true);
            startActivityForResult(CheckDeviceActivity.class, 0, bundle);
            mCheckInstall = true;
        }
    }

    private void checkPort(int tcpPort, int httpPort) {
        RPCRequest request = new RPCRequest();
        request.setId("");
        request.setMethod("device_checkPort");
        request.putInt(tcpPort);
        request.putInt(httpPort);

        String json = request.toJSON();
        String url = String.format(Locale.US, "http://%s:%d", mMiner.getIpString(), mMiner.getPortHttpInt());

        new OKHttpUtils().post(url, json, "device_checkPort", new SimpleCallback<Void>() {
            @Override
            public void onSuccess(okhttp3.Response response, Void result) {
                connectMiner();
            }

            @Override
            public void onFailure(okhttp3.Request request, Exception e) {
                useProxy();
            }

            @Override
            public void onError(okhttp3.Response response, int code, Exception e) {
                useProxy();
            }
        });
    }

    private void useProxy() {
        DeviceInfo.get().setProxy(Config.PROXY_HOST);
        DataServer.getInstance().shutdown();
        stopHttpServer();
        requestTcpPort();
    }

    private void requestTcpPort() {
        mTcpPortProxy = new PortProxy(new ProxyListener(), true);
        mTcpPortProxy.connect();
    }

    private void requestHttpPort() {
        mHttpPortProxy = new PortProxy(new ProxyListener(), false);
        mHttpPortProxy.connect();
    }

    private void closeProxy() {
        if (mTcpPortProxy != null) {
            mTcpPortProxy.close(true);
        }
        if (mHttpPortProxy != null) {
            mHttpPortProxy.close(true);
        }
    }

    private void connectTcpProxy(final int port) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTcpProxy = new TcpProxy();
                mTcpProxy.connect(port);
            }
        });
    }

    private void connectHttpProxy(final int port) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mDispatcher.setProxy(true);
                mHttpProxy = new HttpProxy(mDispatcher);
                mHttpProxy.connect(port);
            }
        });
    }

    private void connectMiner() {
        mDeviceManager = new DeviceManager();
        mDeviceManager.setOnDeviceStateChangedListener(mOnDeviceStateChangedListener);
        mDeviceManager.connect(mMiner);
    }

    private synchronized void startDeviceService() {
        if (!mStartService) {
            silentOn();
            mHandler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            Util.dimOn(getActivity());
                        }
                    }, 30 * 1000);

            mAppManager.setOnTopTaskListener(this);
            mAppManager.setOnAppManagerListener(this);

            DataServer.getInstance().setListener(mConnectionListener);
            DataServer.getInstance().setAppManager(mAppManager);
            try {
                DataServer.getInstance().startServer(mDataPort);
            } catch (Exception e) {
                showAlertDialog(getString(R.string.start_service_failed));
                return;
            }

            mDispatcher = new DefaultRPCDispatcher(getContext(), mMiner);
            mDispatcher.setAppManager(mAppManager);
            mDispatcher.setPromiseHandler(new PromiseHandler(this, mMiner));
            startHttpServer(mDispatcher);

            if (!DeviceInfo.get().isProxyMode()) {
                checkPort(DeviceInfo.get().tcpPort, DeviceInfo.get().httpPort);
            } else {
                useProxy();
            }

            mStartService = true;
            mOrderStateView.setText(R.string.connecting_miners);
        }
    }

    private synchronized void stopDeviceService() {
        if (mStartService) {
            mAppManager.setOnTopTaskListener(null);
            mAppManager.setOnAppManagerListener(null);

            DownloadManager.getInstance().cancelAll();
            stopHttpServer();
            releaseDApp();
            DataServer.getInstance().shutdown();
            if (mDeviceManager != null) {
                mDeviceManager.setOnDeviceStateChangedListener(null);
                mDeviceManager.close();
            }
            closeProxy();

            mStartService = false;
        }

        hideFloatLayer();
    }

    private void startHttpServer(Dispatcher dispatcher) {
        if (mHttpServer == null) {
            try {
                mHttpServer = new HttpServer(mHttpPort, dispatcher);
                mHttpServer.start();
            } catch (Exception e) {
                showAlertDialog(getString(R.string.start_service_failed));
            }
        }
    }

    private void stopHttpServer() {
        if (mHttpServer != null) {
            mHttpServer.stop();
            mHttpServer = null;
        }
    }

    private void startRecordIfNeeded() {
        if (!Touch.getInstance().isRecording()) {
            Touch.getInstance().startRecord(mQuality);
        }
    }

    private void stopRecord() {
        Touch.getInstance().stopRecord();
    }

    private void postRequestPayment(final DApp dApp) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (checkPromiseAmount()) {
                    mTotalTime += Config.REQUEST_PAYMENT_INTERVAL;

                    requestPayment(dApp);
                } else {
                    releaseDApp();
                }
            }
        }, Config.REQUEST_PAYMENT_INTERVAL * 1000);
    }

    private void requestPayment(final DApp dApp) {
        DAppApi.requestPayment(dApp, new Runnable() {
            @Override
            public void run() {
                releaseDApp();
            }
        });
        postRequestPayment(dApp);
    }

    private boolean checkPromiseAmount() {
        if (mDeviceManager.getDapp() == null) {
            return false;
        }
        if (mTotalTime > 0) {
            BigInteger totalAmount = mDeviceManager.getDapp().getAmount(mTotalTime)
                    .multiply(new BigInteger(String.valueOf((int) ((1 - Config.FEE_PERCENT) * 100))))
                    .divide(new BigInteger("100"))
                    .add(mLastAmount);
            if (mReceivedAmount.compareTo(totalAmount) < 0) {
                return false;
            }
        }
        return true;
    }

    private void releaseDApp() {
        if (mDeviceManager != null && mDeviceManager.getDapp() != null) {
            mDeviceManager.releaseDevice();
        }
        onDeviceReleased();
    }

    private void onDeviceReleased() {
        mHandler.removeCallbacksAndMessages(null);

        if (mAppManager != null) {
            mAppManager.clear();
        }
        DataServer.getInstance().releaseDApp();

        mOrderStateView.setText(R.string.wait_for_order);
    }

    private void silentOn() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        }
    }

    private void showAlertDialog(String msg) {
        if (mAlertDialog == null) {
            mAlertDialog = new AlertDialog.Builder(getContext())
                    .setMessage(msg)
                    .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .create();
            mAlertDialog.setCancelable(false);
        }
        mAlertDialog.show();
    }

    private void showExitDialog() {
        new AlertDialog.Builder(getContext())
                .setMessage(getResources().getString(R.string.confirm_to_exit))
                .setNegativeButton(getResources().getString(R.string.cancel), null)
                .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .create()
                .show();
    }

    @Override
    public void onReceivePromise(Promise promise) {
        mReceivedAmount = new BigInteger(promise.getAmount(), 16);

        if (checkPromiseAmount()) {
            BankAllowance bankAllowance = BankAllowance.get();
            BigInteger amount = bankAllowance.amount.multiply(new BigInteger("80")).divide(new BigInteger("100"));
            if (mReceivedAmount.compareTo(amount) > 0) {
                CustomApplication.sInstance.startMonitorService();
            }
        } else {
            finish();
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
        public void onStart(int quality) {
            mQuality = quality;
            startRecordIfNeeded();
        }

        @Override
        public void onStop() {
            stopRecord();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    hideFloatLayer();
                }
            });
            if (!Util.isCharging(getActivity())) {
                finish();
            }
        }

        @Override
        public void onException(Throwable cause) {
        }
    };

    private DeviceManager.OnDeviceStateChangedListener mOnDeviceStateChangedListener = new DeviceManager.OnDeviceStateChangedListener() {
        @Override
        public void onConnected() {
        }

        @Override
        public void onDeviceReady() {
            mOrderStateView.setText(R.string.wait_for_order);
        }

        @Override
        public void onDeviceAssigned(final DApp dApp) {
            mOrderStateView.setText(R.string.received_order);

            if (dApp.priceValid()) {
                Promise promise = Promise.get();
                if (promise != null) {
                    mLastAmount = new BigInteger(promise.getAmount(), 16);
                }
                mTotalTime = 0;

                mAppManager.setDApp(dApp);
                DataServer.getInstance().setDApp(dApp);

                DAppApi.getNonce(Wallet.get().getAddress(), dApp, new Runnable() {
                    @Override
                    public void run() {
                        postRequestPayment(dApp);
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        releaseDApp();
                    }
                });
            } else {
                finish();
            }
        }

        @Override
        public void onDeviceReleased() {
            ReceiveOrderFragment.this.onDeviceReleased();
        }

        @Override
        public void onError(int code, int msg) {
            stopDeviceService();
            showAlertDialog(String.format(getString(msg), code));
        }
    };

    private NetworkHelper.NetworkChangeListener mNetworkChangeListener = new NetworkHelper.NetworkChangeListener() {
        @Override
        public void onNetworkChange(NetworkInfo info) {
            stopDeviceService();
            if (info != null) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mAlertDialog != null) {
                            mAlertDialog.dismiss();
                            mAlertDialog = null;
                        }
                        startDeviceService();
                    }
                }, 500);
            }
        }
    };

    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            stopDeviceService();
            mPaused = true;
        }
    };

    private BaseActivity.OnBackListener mOnBackListener = new BaseActivity.OnBackListener() {
        @Override
        public boolean onBacked() {
            if (mFloatView.getVisibility() == View.VISIBLE) {
                // disable OnBackListener
            } else {
                showExitDialog();
            }
            return true;
        }
    };

    private View.OnClickListener mOnClickExitListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showExitDialog();
        }
    };

    private void registerReceiver() {
        IntentFilter statusIntentFilter = new IntentFilter(Constant.BROADCAST_ACTION_TOUCH_LOCAL);
        mTouchLocalReceiver = new TouchLocalReceiver();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mTouchLocalReceiver,
                statusIntentFilter);

        IntentFilter chargingIntentFilter = new IntentFilter(Constant.BROADCAST_ACTION_CHARGING);
        mChargingReceiver = new ChargingReceiver();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mChargingReceiver,
                chargingIntentFilter);

        IntentFilter stateChangedIntentFilter = new IntentFilter(Constant.BROADCAST_ACTION_STATE_CHANGED);
        mStateChangedReceiver = new MinerStateChangedReceiver();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mStateChangedReceiver,
                stateChangedIntentFilter);
    }

    private void unregisterReceiver() {
        if (mTouchLocalReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mTouchLocalReceiver);
            mTouchLocalReceiver = null;
        }
        if (mChargingReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mChargingReceiver);
            mChargingReceiver = null;
        }
        if (mStateChangedReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mStateChangedReceiver);
            mStateChangedReceiver = null;
        }
    }

    private class TouchLocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!TextUtils.isEmpty(action)) {
                switch (intent.getAction()) {
                    case Constant.BROADCAST_ACTION_TOUCH_LOCAL:
                        finish();
                        break;

                    default:
                        break;
                }
            }
        }
    }

    private class ChargingReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isCharging = intent.getBooleanExtra(Constant.EXTENDED_DATA_CHARGING, true);
            if (!isCharging && mAppManager.getState() != AppManager.State.LAUNCHING
                    && mAppManager.getState() != AppManager.State.LAUNCHED) {
                finish();
            }
        }
    }

    private class MinerStateChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.STATE_INVALID.equals(intent.getAction())) {
                stopDeviceService();
                showAlertDialog(getString(R.string.invalid_miner));
            }
        }
    }

    private class ProxyListener implements PortProxy.Listener {
        @Override
        public void onPort(int port, boolean tcp) {

            if (tcp) {
                DeviceInfo.get().tcpPort = port;
                requestHttpPort();
            } else {
                DeviceInfo.get().httpPort = port;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        connectMiner();
                    }
                });
            }
        }

        @Override
        public void onProxy(final int port, boolean tcp) {

            if (tcp) {
                connectTcpProxy(port);
            } else {
                connectHttpProxy(port);
            }
        }

        @Override
        public void onException(Throwable cause) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    showAlertDialog(String.format(getString(R.string.connect_miner_failed), ErrorCode.CONNECT_PROXY_FAILED));
                }
            });
        }
    }
}
