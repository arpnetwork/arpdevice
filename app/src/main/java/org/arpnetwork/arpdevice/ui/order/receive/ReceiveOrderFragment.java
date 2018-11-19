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

import org.arpnetwork.adb.ShellChannel;
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
import org.arpnetwork.arpdevice.data.AppInfo;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.device.Adb;
import org.arpnetwork.arpdevice.device.DeviceManager;
import org.arpnetwork.arpdevice.device.TaskHelper;
import org.arpnetwork.arpdevice.download.DownloadManager;
import org.arpnetwork.arpdevice.proxy.PortProxy;
import org.arpnetwork.arpdevice.proxy.TcpProxy;
import org.arpnetwork.arpdevice.rpc.RPCRequest;
import org.arpnetwork.arpdevice.server.DataServer;
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
import java.util.LinkedList;
import java.util.Locale;
import java.util.Random;

public class ReceiveOrderFragment extends BaseFragment implements PromiseHandler.OnReceivePromiseListener,
        TaskHelper.OnTopTaskListener, AppManager.OnAppManagerListener {
    private static final String TAG = ReceiveOrderFragment.class.getSimpleName();

    private static final int SCREEN_BRIGHTNESS_WAITING = 20;
    private static final String KEY_PORT = "ports";
    private static final String KEY_BRIGHT = "bright";
    private static final String KEY_BRIGHT_MODE = "brightness_mode";

    private TextView mOrderStateView;
    private View mFloatView;

    private DeviceManager mDeviceManager;
    private AppManager mAppManager;
    private Miner mMiner;

    private PromiseHandler mPromiseHandler;
    private PortProxy mTcpPortProxy;
    private TcpProxy mTcpProxy;

    private BigInteger mLastAmount = BigInteger.ZERO;
    private BigInteger mReceivedAmount = BigInteger.ZERO;
    private int mTcpPort;
    private long mLaunchTime;
    private int mQuality;
    private int mTotalTime;
    private int mRestartServiceNum;
    private boolean mStartService;
    private boolean mCheckInstall;
    private boolean mInstallSuccess;

    private TouchLocalReceiver mTouchLocalReceiver;
    private ChargingReceiver mChargingReceiver;
    private MinerStateChangedReceiver mStateChangedReceiver;
    private ScreenStateChangedReceiver mScreenStateChangedReceiver;

    private Dialog mAlertDialog;
    private Dialog mExitAlertDialog;
    private Handler mHandler = new Handler();

    private int mScreenBrightnessMode = -1; // 0: close 1:open
    private int mScreenBrightness = -1; // 0-255

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_PORT, mTcpPort);
        if (mScreenBrightness != -1) {
            outState.putInt(KEY_BRIGHT, mScreenBrightness);
        }
        if (mScreenBrightnessMode != -1) {
            outState.putInt(KEY_BRIGHT_MODE, mScreenBrightnessMode);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_receive_order, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews();
        startDeviceService();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCheckInstall) {
            startDeviceService();
            mCheckInstall = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        globalDimOff();
        OKHttpUtils.cancelAll();
        mHandler.removeCallbacksAndMessages(null);
        NetworkHelper.getInstance().unregisterNetworkListener(mNetworkChangeListener);
        stopDeviceService();
        unregisterReceiver();

        hideExitDialog();
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }

        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            mInstallSuccess = true;

            Touch.getInstance().openTouch();
            globalDimOn(SCREEN_BRIGHTNESS_WAITING);
        }
    }

    @Override
    public void onTopTaskIllegal(String pkgName) {
        DataServer.getInstance().onClientDisconnected();

        long now = System.currentTimeMillis();
        if (now - mLaunchTime < 2000) {
            DAppApi.appStop(pkgName, 0, mDeviceManager.getDapp());
        }
    }

    @Override
    public void onAppInstall(boolean success) {
        if (!success && !mInstallSuccess) {
            startCheckActivity();
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

    private void init(Bundle savedInstanceState) {
        Touch.getInstance().openTouch();

        setTitle(R.string.receive_order);
        getBaseActivity().setOnBackListener(mOnBackListener);

        mMiner = (Miner) getArguments().getSerializable(Constant.KEY_MINER);

        int restorePort;
        if (savedInstanceState != null && (restorePort = savedInstanceState.getInt(KEY_PORT)) > 0) {
            DeviceInfo.get().setDataPort(restorePort);
            mTcpPort = restorePort;
        } else {
            mTcpPort = DeviceInfo.get().tcpPort;
        }
        mAppManager = AppManager.getInstance(getContext().getApplicationContext());
        mPromiseHandler = new PromiseHandler(this, mMiner);

        registerReceiver();
        NetworkHelper.getInstance().registerNetworkListener(mNetworkChangeListener);

        if (savedInstanceState == null) {
            getGlobalBright();
        } else {
            mScreenBrightness = savedInstanceState.getInt(KEY_BRIGHT, -1);
            mScreenBrightnessMode = savedInstanceState.getInt(KEY_BRIGHT_MODE, -1);
        }
    }

    private void initViews() {
        mOrderStateView = (TextView) findViewById(R.id.tv_order_state);
        mOrderStateView.setText(R.string.starting_service);
        mFloatView = findViewById(R.id.view_float);

        Button exitButton = (Button) findViewById(R.id.btn_exit);
        exitButton.setOnClickListener(mOnClickExitListener);
    }

    private void startCheckActivity() {
        if (!mCheckInstall) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(Constant.KEY_FROM_ORDER, true);
            startActivityForResult(CheckDeviceActivity.class, 0, bundle);
            mCheckInstall = true;

            Touch.getInstance().closeTouch();
            globalDimOff();
            stopDeviceService();
        }
    }

    private void checkPort(int tcpPort) {
        RPCRequest request = new RPCRequest();
        request.setId(String.valueOf(new Random().nextInt(Integer.MAX_VALUE)));
        request.setMethod("device_checkPort");
        request.putInt(tcpPort);

        String json = request.toJSON();
        String url = String.format(Locale.US, "http://%s:%d", mMiner.getIpString(), mMiner.getPortHttpInt());

        new OKHttpUtils().post(url, json, "checkPort", new SimpleCallback<Void>() {
            @Override
            public void onSuccess(okhttp3.Response response, Void result) {
                DeviceInfo.get().setProxy(null);
                DeviceInfo.get().setDataPort(mTcpPort);
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
        DataServer.getInstance().close(true);
        requestTcpPort();
    }

    private void requestTcpPort() {
        mTcpPortProxy = new PortProxy(new ProxyListener(), true);
        mTcpPortProxy.connect();
    }

    private void closeProxy() {
        if (mTcpPortProxy != null) {
            mTcpPortProxy.close(true);
        }
    }

    private void connectTcpProxy(final int port, final byte[] session) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTcpProxy = new TcpProxy(session);
                mTcpProxy.connect(port);
            }
        });
    }

    private void connectMiner() {
        DeviceInfo.get().address = Wallet.getAccountAddress();

        mDeviceManager = new DeviceManager();
        mDeviceManager.setOnDeviceStateChangedListener(mOnDeviceStateChangedListener);
        mDeviceManager.connect(mMiner);
    }

    private synchronized void startDeviceService() {
        if (!mStartService) {
            silentOn();
            globalDimOnDelayed();

            mAppManager.setOnTopTaskListener(this);
            mAppManager.setOnAppManagerListener(this);
            DataServer.getInstance().setListener(mConnectionListener);
            DataServer.getInstance().setAppManager(mAppManager);

            if (mTcpPort == 0) {
                useProxy();
            } else {
                try {
                    DataServer.getInstance().close(true);
                    DataServer.getInstance().startServer(mTcpPort);
                } catch (Exception e) {
                    showAlertDialog(getString(R.string.start_service_failed));
                    return;
                }
                checkPort(mTcpPort);
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
            releaseDApp();
            DataServer.getInstance().setListener(null);
            DataServer.getInstance().close(true);
            if (mDeviceManager != null) {
                mDeviceManager.setOnDeviceStateChangedListener(null);
                mDeviceManager.close();
            }
            closeProxy();

            mStartService = false;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mOrderStateView.setText(R.string.miner_disconnected);
                }
            });
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

    private void restartService() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                stopDeviceService();
                mOrderStateView.setText(R.string.connecting_miners);

                long delay;
                if (mRestartServiceNum == 0) {
                    delay = 3000;
                } else if (mRestartServiceNum == 1) {
                    delay = 5000;
                } else if (mRestartServiceNum == 2) {
                    delay = 10000;
                } else {
                    delay = 30000;
                }
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startDeviceService();
                    }
                }, delay);
                mRestartServiceNum++;
            }
        });
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
        if (mStartService) {
            if (mDeviceManager != null && mDeviceManager.getDapp() != null) {
                mDeviceManager.releaseDevice();
            }
            onDeviceReleased();
        }
    }

    private void onDeviceReleased() {
        mHandler.removeCallbacksAndMessages(null);

        if (mAppManager != null) {
            mAppManager.clear();
        }
        DataServer.getInstance().releaseDApp();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mOrderStateView.setText(R.string.wait_for_order);
                hideFloatLayer();
            }
        });
    }

    private void silentOn() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        }
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

    private void getGlobalBright() {
        final Adb adb = new Adb(Touch.getInstance().getConnection());
        adb.getGlobalBright(new ShellChannel.ShellListener() {
            final LinkedList<String> items = new LinkedList<String>();

            @Override
            public void onStdout(ShellChannel ch, byte[] data) {
                // 1
                // 89
                String item = new String(data).trim();
                items.add(item);
                if (items.size() > 1) {
                    mScreenBrightnessMode = Integer.parseInt(items.get(0).trim());
                    mScreenBrightness = Integer.parseInt(items.get(1).trim());
                }
            }

            @Override
            public void onStderr(ShellChannel ch, byte[] data) {
            }

            @Override
            public void onExit(ShellChannel ch, int code) {
            }
        });
    }

    private void globalDimOnDelayed() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mCheckInstall) {
                    globalDimOn(SCREEN_BRIGHTNESS_WAITING);
                }
            }
        }, 30 * 1000);
    }

    private void globalDimOn(int screenBrightness) {
        Adb adb = new Adb(Touch.getInstance().getConnection());
        adb.globalDimOn(screenBrightness);
    }

    private void globalDimOff() {
        Adb adb = new Adb(Touch.getInstance().getConnection());
        adb.globalDimRestore(mScreenBrightnessMode, mScreenBrightness);
    }

    private void showAlertDialog(String msg) {
        if (mAlertDialog == null && getContext() != null) {
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
        if (mExitAlertDialog == null) {
            mExitAlertDialog = new AlertDialog.Builder(getContext())
                    .setMessage(getResources().getString(R.string.confirm_to_exit))
                    .setNegativeButton(getResources().getString(R.string.cancel), null)
                    .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .create();
            mExitAlertDialog.setCancelable(false);
        }
        mExitAlertDialog.show();
    }

    private void hideExitDialog() {
        if (mExitAlertDialog != null && mExitAlertDialog.isShowing()) {
            mExitAlertDialog.dismiss();
            mExitAlertDialog = null;
        }
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
            float batteryPct = Util.getBatteryPct(getContext());
            if (!Util.isCharging(getActivity()) && batteryPct < 0.5f) {
                finish();
            }
        }

        @Override
        public void onException(Throwable cause) {
            restartService();
        }
    };

    private DeviceManager.OnDeviceStateChangedListener mOnDeviceStateChangedListener = new DeviceManager.OnDeviceStateChangedListener() {
        @Override
        public void onConnected() {
            mRestartServiceNum = 0;
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

                DAppApi.getNonce(Wallet.getAccountAddress(), dApp, new Runnable() {
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
        public void onPromiseReceived(Promise promise) {
            mPromiseHandler.processPromise(promise);
        }

        @Override
        public void onAppInstall(AppInfo info) {
            mAppManager.appInstall(info);
        }

        @Override
        public void onAppUninstall(String pkgName) {
            mAppManager.uninstallApp(pkgName);
        }

        @Override
        public void onAppStart(String pkgName) {
            mAppManager.startApp(pkgName);
        }

        @Override
        public void onError(int code, int msg) {
            if (getContext() == null) {
                return;
            }
            if (code == ErrorCode.NETWORK_ERROR) {
                restartService();
            } else {
                stopDeviceService();
                showAlertDialog(String.format(getString(msg), code));
            }
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

    private BaseActivity.OnBackListener mOnBackListener = new BaseActivity.OnBackListener() {
        @Override
        public boolean onBacked() {
            if (mFloatView.getVisibility() == View.VISIBLE) {
                // disable OnBackListener
                hideExitDialog();
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

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        mScreenStateChangedReceiver = new ScreenStateChangedReceiver();
        getActivity().registerReceiver(mScreenStateChangedReceiver, filter);
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
        if (mScreenStateChangedReceiver != null) {
            getActivity().unregisterReceiver(mScreenStateChangedReceiver);
            mScreenStateChangedReceiver = null;
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
            float batteryPct = Util.getBatteryPct(getContext());

            if (!isCharging && batteryPct < 0.5 && mAppManager.getState() != AppManager.State.LAUNCHING
                    && mAppManager.getState() != AppManager.State.LAUNCHED) {
                finish();
            }
        }
    }

    private class MinerStateChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.BROADCAST_ACTION_STATE_CHANGED.equals(intent.getAction())) {
                stopDeviceService();
                showAlertDialog(getString(R.string.invalid_miner));
            }
        }
    }

    private class ScreenStateChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                finish();
            }
        }
    }

    private class ProxyListener implements PortProxy.Listener {
        @Override
        public void onPort(int proxyPort, boolean tcp) {
            if (tcp) {
                DeviceInfo.get().setDataPort(proxyPort);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        connectMiner();
                    }
                });
            }
        }

        @Override
        public void onHandshake(int acceptPort, byte[] session, boolean tcp) {
            if (tcp) {
                connectTcpProxy(acceptPort, session);
            }
        }

        @Override
        public void onException(Throwable cause) {
            restartService();
        }
    }
}
