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
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.app.AppManager;
import org.arpnetwork.arpdevice.app.DAppApi;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.contracts.ARPRegistry;
import org.arpnetwork.arpdevice.contracts.tasks.OnValueResult;
import org.arpnetwork.arpdevice.data.BankAllowance;
import org.arpnetwork.arpdevice.data.DApp;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.device.DeviceManager;
import org.arpnetwork.arpdevice.device.TaskHelper;
import org.arpnetwork.arpdevice.download.DownloadManager;
import org.arpnetwork.arpdevice.server.DataServer;
import org.arpnetwork.arpdevice.server.http.Dispatcher;
import org.arpnetwork.arpdevice.server.http.HttpServer;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.ui.base.BaseActivity;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.miner.BindMinerHelper;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.util.UIHelper;

import java.math.BigInteger;
import java.util.Timer;
import java.util.TimerTask;

public class ReceiveOrderFragment extends BaseFragment implements PromiseHandler.OnReceivePromiseListener {
    private static final String TAG = ReceiveOrderFragment.class.getSimpleName();

    private static final int PERIOD = 3600000;

    private DeviceManager mDeviceManager;
    private HttpServer mHttpServer;
    private AppManager mAppManager;
    private DApp mDApp;
    private Timer mTimer;

    private BigInteger mReceivedAmount = new BigInteger("0");
    private int mQuality;
    private int mTotalTime;
    private boolean mRetryRequestPayment;

    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.receive_order);

        getBaseActivity().setOnBackListener(mOnBackListener);
        startDeviceService();
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
    public void onDestroy() {
        stopDeviceService();

        super.onDestroy();
    }

    private void initViews() {
        Button exitButton = (Button) findViewById(R.id.btn_exit);
        exitButton.setOnClickListener(mOnClickExitListener);
    }

    private void startDeviceService() {
        TaskHelper taskHelper = new TaskHelper();

        DataServer.getInstance().setListener(mConnectionListener);
        DataServer.getInstance().setTaskHelper(taskHelper);
        DataServer.getInstance().startServer();

        mAppManager = new AppManager(DataServer.getInstance().getHandler(), taskHelper);

        mDeviceManager = new DeviceManager();
        mDeviceManager.setOnDeviceAssignedListener(mOnManageDeviceListener);
        mDeviceManager.setOnErrorListener(mOnErrorListener);
        mDeviceManager.connect();

        DefaultRPCDispatcher defaultRPCDispatcher = new DefaultRPCDispatcher(getContext());
        defaultRPCDispatcher.setAppManager(mAppManager);
        defaultRPCDispatcher.setPromiseHandler(new PromiseHandler(this));
        startHttpServer(defaultRPCDispatcher);

        startTimer();
    }

    private void stopDeviceService() {
        stopTimer();
        mHandler.removeCallbacksAndMessages(null);
        DownloadManager.getInstance().cancelAll();
        stopHttpServer();
        DataServer.getInstance().shutdown();
        if (mDeviceManager != null) {
            mDeviceManager.close();
        }
    }

    private void startHttpServer(Dispatcher dispatcher) {
        try {
            mHttpServer = new HttpServer(Config.HTTP_SERVER_PORT, dispatcher);
            mHttpServer.start();
        } catch (Exception e) {
        }
    }

    private void stopHttpServer() {
        if (mHttpServer != null) {
            mHttpServer.stop();
        }
    }

    private void startRecordIfNeeded() {
        if (Touch.getInstance().isRecording()) return;
        Touch.getInstance().startRecord(mQuality);
    }

    private void stopRecord() {
        Touch.getInstance().stopRecord();
    }

    private void startTimer() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                loadAllowance();
            }
        }, 0, PERIOD);
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private void loadAllowance() {
        String spender = Wallet.get().getAddress();
        Miner miner = BindMinerHelper.getBound(spender);
        ARPBank.allowanceARP(miner.getAddress(), spender, new OnValueResult<BankAllowance>() {
            @Override
            public void onValueResult(BankAllowance result) {
                if (result != null) {
                    if (!TextUtils.isEmpty(result.proxy)
                            && (result.proxy.equals("0x0000000000000000000000000000000000000000") || result.proxy.equals(ARPRegistry.CONTRACT_ADDRESS))
                            && (result.expired.longValue() == 0 || result.expired.longValue() >= (System.currentTimeMillis() / 1000 + 24 * 60 * 60))) {
                        result.save();
                    } else {
                        finish();
                    }
                }
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
                    DataServer.getInstance().releaseDApp();
                }
            }
        }, Config.REQUEST_PAYMENT_INTERVAL * 1000);
    }

    private void requestPayment(final DApp dApp) {
        DAppApi.requestPayment(dApp, new Runnable() {
            @Override
            public void run() {
                mRetryRequestPayment = false;
            }
        }, new Runnable() {
            @Override
            public void run() {
                if (!mRetryRequestPayment) {
                    DAppApi.requestPayment(dApp, null, this);
                    mRetryRequestPayment = true;
                }
            }
        });
        postRequestPayment(dApp);
    }

    private boolean checkPromiseAmount() {
        if (mTotalTime > 0) {
            BigInteger totalAmount = mDApp.getAmount(mTotalTime)
                    .multiply(new BigInteger(String.valueOf((int) ((1 - Config.FEE_PERCENT) * 100))))
                    .divide(new BigInteger("100"));
            if (mReceivedAmount.compareTo(totalAmount) < 0) {
                return false;
            }
        }
        return true;
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

        if (!checkPromiseAmount()) {
            DataServer.getInstance().releaseDApp();
            mHandler.removeCallbacksAndMessages(null);
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

    private DeviceManager.OnManageDeviceListener mOnManageDeviceListener = new DeviceManager.OnManageDeviceListener() {
        @Override
        public void onDeviceAssigned(final DApp dApp) {
            if (dApp.priceValid()) {
                mDApp = dApp;
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
                        finish();
                    }
                });
            } else {
                finish();
            }
        }

        @Override
        public void onDeviceReleased() {
            mDApp = null;
            mAppManager.clear();
            DataServer.getInstance().releaseDApp();
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

    private BaseActivity.OnBackListener mOnBackListener = new BaseActivity.OnBackListener() {
        @Override
        public boolean onBacked() {
            showExitDialog();
            return true;
        }
    };

    private View.OnClickListener mOnClickExitListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showExitDialog();
        }
    };
}
