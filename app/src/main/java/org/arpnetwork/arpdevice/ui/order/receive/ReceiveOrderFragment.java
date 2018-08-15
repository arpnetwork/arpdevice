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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.app.AppManager;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.data.DApp;
import org.arpnetwork.arpdevice.device.DeviceManager;
import org.arpnetwork.arpdevice.device.TaskHelper;
import org.arpnetwork.arpdevice.download.DownloadManager;
import org.arpnetwork.arpdevice.server.DataServer;
import org.arpnetwork.arpdevice.server.http.Dispatcher;
import org.arpnetwork.arpdevice.server.http.HttpServer;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.ui.base.BaseActivity;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.util.UIHelper;

public class ReceiveOrderFragment extends BaseFragment {
    private int mQuality;
    private DeviceManager mDeviceManager;
    private HttpServer mHttpServer;

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

        final AppManager appManager = new AppManager(DataServer.getInstance().getHandler(), taskHelper);

        mDeviceManager = new DeviceManager();
        mDeviceManager.setOnDeviceAssignedListener(new DeviceManager.OnManageDeviceListener() {
            @Override
            public void onDeviceAssigned(DApp dApp) {
                appManager.setDApp(dApp);
                DataServer.getInstance().setDApp(dApp);
            }

            @Override
            public void onDeviceReleased() {
                appManager.setDApp(null);
                DataServer.getInstance().releaseDApp();
            }
        });
        mDeviceManager.setOnErrorListener(mOnErrorListener);
        mDeviceManager.connect();

        DefaultRPCDispatcher defaultRPCDispatcher = new DefaultRPCDispatcher(getContext());
        defaultRPCDispatcher.setAppManager(appManager);
        startHttpServer(defaultRPCDispatcher);
    }

    private void stopDeviceService() {
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
