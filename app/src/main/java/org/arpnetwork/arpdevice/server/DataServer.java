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

package org.arpnetwork.arpdevice.server;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.device.DeviceManager;
import org.arpnetwork.arpdevice.device.TaskHelper;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.data.ChangeQualityReq;
import org.arpnetwork.arpdevice.data.ConnectReq;
import org.arpnetwork.arpdevice.data.Message;
import org.arpnetwork.arpdevice.data.ProtocolPacket;
import org.arpnetwork.arpdevice.data.Req;
import org.arpnetwork.arpdevice.data.TouchSetting;
import org.arpnetwork.arpdevice.data.VideoInfo;

import java.util.concurrent.LinkedBlockingQueue;

import io.netty.buffer.ByteBuf;

public final class DataServer implements NettyConnection.ConnectionListener {
    public static final int PORT = 9000;

    private static final String TAG = "DataServer";
    private static final boolean DEBUG = Config.DEBUG;
    private static final int HEARTBEAT_TIMEOUT = 10000;
    private static final int HEARTBEAT_INTERVAL = 5000;
    private static final int CONNECTED_TIMEOUT = 10000;
    private static volatile DataServer sInstance;

    private final LinkedBlockingQueue<ByteBuf> mPacketQueue = new LinkedBlockingQueue<ByteBuf>();
    private Handler mHandler = new Handler();

    private DeviceManager mDeviceManager;
    private Gson mGson;

    private SendThread mAVDataThread;
    private int mQuality;
    private boolean mReceiveDisconnect;
    private TaskHelper mTaskHelper;

    private NettyConnection mConn;

    private ConnectionListener mListener;

    public interface ConnectionListener {
        void onConnected();

        void onClosed();

        void onRecordStart(int quality);

        void onRecordStop();

        void onException(Throwable cause);
    }

    public static DataServer getInstance() {
        if (sInstance == null) {
            synchronized (DataServer.class) {
                if (sInstance == null) {
                    sInstance = new DataServer();
                }
            }
        }
        return sInstance;
    }

    public void setListener(ConnectionListener listener) {
        mListener = listener;
    }

    public void setDeviceManager(DeviceManager deviceManager) {
        mDeviceManager = deviceManager;
        mDeviceManager.setOnClientRequestListener(mOnClientRequestListener);
    }

    public void startServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mConn.startServer();
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
    }

    public void shutdown() {
        mDeviceManager.setOnClientRequestListener(null);
        new Thread(new Runnable() {
            @Override
            public void run() {
                mConn.shutdown();
            }
        }).start();
    }

    public void onClientDisconnected() {
        stop();
    }

    /**
     * Called by RecordService.
     */
    public void onVideoChanged(int width, int height, int quality) {
        VideoInfo videoInfo = new VideoInfo(width, height, quality);
        Message pkt = ProtocolPacket.generateProtocol(ProtocolPacket.VIDEO_CHANGED, 0, videoInfo);
        mConn.write(pkt);
    }

    public void enqueueAVPacket(ByteBuf byteBuf) {
        mPacketQueue.add(byteBuf);
    }

    @Override
    public void onConnected(NettyConnection conn) {
        mReceiveDisconnect = false;
        if (mDeviceManager == null || !mDeviceManager.isRegistered()) {
            stop();
            return;
        }

        startHeartbeatTimer();
        startHeartbeatTimeout();
    }

    @Override
    public void onClosed(NettyConnection conn) {
        if (!mReceiveDisconnect) {
            stop();
            mDeviceManager.notifyServer(DeviceManager.FINISHED);
        }
    }

    @Override
    public void onMessage(NettyConnection conn, Message msg) throws Exception {
        switch (msg.getType()) {
            case Message.HEARTBEAT:
                onReceiveHeartbeat();
                break;

            case Message.TIME:
                onReceiveTimestamp(msg.parsePacket(Long.class));
                break;

            case Message.TOUCH:
                onClientMinitouchData(msg.parsePacket(String.class));
                break;

            case Message.PROTOCOL:
                processProtocolPacket(msg.parsePacket(String.class));
                break;

            default:
                break;
        }
    }

    @Override
    public void onException(NettyConnection conn, Throwable cause) {
        Log.e(TAG, "onException. cause = " + cause.getMessage());
    }

    private DataServer() {
        mConn = new NettyConnection(this, PORT);
        mGson = new Gson();
    }

    private void onReceiveHeartbeat() {
        stopHeartbeatTimeout();
        startHeartbeatTimeout();
    }

    private void onReceiveTimestamp(long clientTime) {
        // fixme: add time delay with client.
        /*Message pkt = ProtocolPacket.generateTimestamp(clientTime);
        mConn.write(pkt);*/
    }

    private void onClientMinitouchData(String cmd) {
        Touch.getInstance().sendTouch(cmd);
    }

    private void processProtocolPacket(String protocolJson) {
        Req req = mGson.fromJson(protocolJson, Req.class);
        switch (req.id) {
            case ProtocolPacket.CONNECT_REQ:
                mHandler.removeCallbacks(mConnectedTimeoutRunnable);

                final ConnectReq connectReq = mGson.fromJson(protocolJson, ConnectReq.class);
                if (!protocolCompatible(connectReq)) {
                    Message pkt = ProtocolPacket.generateProtocol(ProtocolPacket.CONNECT_RESP, ProtocolPacket.RESULT_NOT_SUPPORT, null);
                    mConn.write(pkt);
                    stop();
                } else if (!verifySession(connectReq)) {
                    stop(); // close client.
                } else {
                    mTaskHelper = new TaskHelper(mHandler);
                    onConnectFirstReq(connectReq);
                    start();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!mTaskHelper.launchApp(connectReq.data.packageName)) {
                                mHandler.removeCallbacks(mConnectedTimeoutRunnable);
                                stop(); // close client.
                            }
                        }
                    }, 500);
                }
                break;

            case ProtocolPacket.DISCONNECT_REQ:
                onReceiveDisconnect();
                break;

            case ProtocolPacket.CHANGE_QUALITY_REQ:
                ChangeQualityReq changeQualityReq = mGson.fromJson(protocolJson, ChangeQualityReq.class);
                onChangeQualityReq(changeQualityReq);
                break;

            default:
                break;
        }
    }

    private boolean protocolCompatible(ConnectReq req) {
        return Config.PROTOCOL_VERSION.equals(req.data.version);
    }

    private boolean verifySession(ConnectReq req) {
        return !TextUtils.isEmpty(req.data.session) && req.data.session.equals(mDeviceManager.getSession());
    }

    private void onConnectFirstReq(ConnectReq req) {
        mQuality = req.data.quality;

        Message pkt = ProtocolPacket.generateProtocol(ProtocolPacket.CONNECT_RESP, 0, null);
        mConn.write(pkt);

        mDeviceManager.notifyServer(DeviceManager.CONNECTED);
    }

    private void onReceiveDisconnect() {
        mReceiveDisconnect = true;
        Message pkt = ProtocolPacket.generateProtocol(ProtocolPacket.DISCONNECT_RESP, 0, null);
        mConn.write(pkt);

        stop();
        mDeviceManager.notifyServer(DeviceManager.FINISHED);
    }

    private void onChangeQualityReq(ChangeQualityReq req) {
        mQuality = req.data.quality;
        // TODO: change quality need sync to execute.

        Message pkt = ProtocolPacket.generateProtocol(ProtocolPacket.CHANGE_QUALITY_RESP, 0, null);
        mConn.write(pkt);
    }

    /**
     * Send arptouch info when connected.
     */
    private void onMinitouchData() {
        TouchSetting touchSetting = TouchSetting.createTouchSetting();
        Message pkt = ProtocolPacket.generateProtocol(ProtocolPacket.TOUCH_INFO, 0, touchSetting);
        mConn.write(pkt);
    }

    private void start() {
        onMinitouchData();

        // start record service.
        if (mListener != null) {
            mListener.onRecordStart(mQuality);
        }

        mAVDataThread = new SendThread();
        mAVDataThread.start();
    }

    private void stop() {
        if (mListener != null) {
            mListener.onRecordStop();
        }
        if (mAVDataThread != null) {
            mAVDataThread.interrupt();
            mAVDataThread.cancel();
            mAVDataThread = null;
        }

        // kill apk
        if (mTaskHelper != null) {
            mTaskHelper.killLaunchedApp();
        }

        mPacketQueue.clear();
        mConn.closeConnection();

        // fix client terminate with no touch up.
        Touch.getInstance().sendTouch("r\n");

        stopHeartbeatTimer();
        stopHeartbeatTimeout();
    }

    private class SendThread extends Thread {
        private boolean mStopped;

        @Override
        public void run() {
            mPacketQueue.clear();

            while (!mStopped) {
                try {
                    ByteBuf packet = mPacketQueue.take();
                    send(packet);
                } catch (InterruptedException e) {
                    Log.e(TAG, "SendThread interrupted.");
                } catch (Exception e) {
                    Log.e(TAG, "SendThread failed.");
                }
            }
        }

        private void send(ByteBuf packet) {
            mConn.write(new Message(packet));
        }

        private void cancel() {
            mStopped = true;
        }
    }

    private final Runnable mServerHeartTimeout = new Runnable() {
        @Override
        public void run() {
            Message pkt = ProtocolPacket.generateHeartbeat();
            mConn.write(pkt);

            mHandler.postDelayed(this, HEARTBEAT_INTERVAL);
        }
    };

    private void startHeartbeatTimer() {
        mHandler.postDelayed(mServerHeartTimeout, HEARTBEAT_INTERVAL);
    }

    private void stopHeartbeatTimer() {
        mHandler.removeCallbacks(mServerHeartTimeout);
    }

    private final Runnable mClientHeartTimeout = new Runnable() {
        @Override
        public void run() {
            onClientDisconnected();
        }
    };

    private void startHeartbeatTimeout() {
        mHandler.postDelayed(mClientHeartTimeout, HEARTBEAT_TIMEOUT);
    }

    private void stopHeartbeatTimeout() {
        mHandler.removeCallbacks(mClientHeartTimeout);
    }

    private DeviceManager.OnClientRequestListener mOnClientRequestListener = new DeviceManager.OnClientRequestListener() {
        @Override
        public void onClientRequest() {
            mHandler.postDelayed(mConnectedTimeoutRunnable, CONNECTED_TIMEOUT);
        }
    };

    private Runnable mConnectedTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            stop();
            mDeviceManager.notifyServer(DeviceManager.CONNECTED_TIMEOUT);
        }
    };
}
