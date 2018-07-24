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

package org.arpnetwork.arpdevice;

import android.net.ConnectivityManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.arpnetwork.arpdevice.device.DeviceManager;
import org.arpnetwork.arpdevice.server.DataServer;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.util.UIHelper;

import org.arpnetwork.arpdevice.data.DeviceInfo;
import org.arpnetwork.arpdevice.opengl.GLRenderer;
import org.arpnetwork.arpdevice.util.NetworkHelper;

import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity {
    private int mQuality;

    private DeviceManager mDeviceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        DataServer.getInstance().setListener(mConnectionListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDeviceManager != null) {
            mDeviceManager.close();
        }
        getApplication().onTerminate();
    }

    private void initViews() {
        GLSurfaceView surfaceView = findViewById(R.id.gl_surface);
        surfaceView.setEGLContextClientVersion(1);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        surfaceView.setRenderer(new GLRenderer(new GLRenderer.Callback() {
            @Override
            public void onSurfaceCreated(GL10 gl) {
                String glRenderer = gl.glGetString(GL10.GL_RENDERER);

                DeviceInfo info = DeviceInfo.get();
                info.gpu = glRenderer;
                int type = NetworkHelper.getInstance().getNetworkType();
                info.connNetType = type;
                if (type == ConnectivityManager.TYPE_MOBILE) {
                    info.telNetType = NetworkHelper.getTelephonyNetworkType(getApplicationContext());
                }

                startDeviceService();
            }
        }));
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

    private DataServer.ConnectionListener mConnectionListener = new DataServer.ConnectionListener() {
        @Override
        public void onConnected() {
        }

        @Override
        public void onClosed() {
            MainActivity.this.stopRecord();
        }

        @Override
        public void onRecordStart(int quality) {
            mQuality = quality;
            MainActivity.this.startRecordIfNeeded();
        }

        @Override
        public void onRecordStop() {
            MainActivity.this.stopRecord();
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
                    UIHelper.showToast(MainActivity.this, msg, Toast.LENGTH_SHORT);
                }
            });
        }
    };
}
