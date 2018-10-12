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

import com.google.gson.Gson;

import org.arpnetwork.arpdevice.app.AppManager;
import org.arpnetwork.arpdevice.app.DAppApi;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.data.DApp;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.data.ChangeQualityReq;
import org.arpnetwork.arpdevice.data.ConnectReq;
import org.arpnetwork.arpdevice.data.Message;
import org.arpnetwork.arpdevice.data.ProtocolPacket;
import org.arpnetwork.arpdevice.data.Req;
import org.arpnetwork.arpdevice.data.TouchSetting;
import org.arpnetwork.arpdevice.data.VideoInfo;

import java.lang.ref.SoftReference;
import java.util.concurrent.LinkedBlockingQueue;

import io.netty.buffer.ByteBuf;

public final class DataServer implements NettyConnection.ConnectionListener {
    //    public static final int MSG_CONNECTED_TIMEOUT = 1;
//    public static final int MSG_LAUNCH_APP_SUCCESS = MSG_CONNECTED_TIMEOUT + 1;
//    public static final int MSG_LAUNCH_APP_FAILED = MSG_LAUNCH_APP_SUCCESS + 1;
    public static final int CONNECTED_TIMEOUT = 10000;

    private static final String TAG = "DataServer";
    private static final boolean DEBUG = Config.DEBUG;

    private static final int HEARTBEAT_TIMEOUT = 15000;
    private static final int HEARTBEAT_INTERVAL = 5000;

    private static volatile DataServer sInstance;

    private final LinkedBlockingQueue<ByteBuf> mPacketQueue = new LinkedBlockingQueue<ByteBuf>();
    private Handler mHandler;

    private Gson mGson;
    private DApp mDApp;
    private String mSession;

    private SendThread mAVDataThread;
    private int mQuality;
    private boolean mStop;
    private AppManager mAppManager;

    private NettyConnection mConn;

    private ConnectionListener mListener;

    public interface ConnectionListener {
        void onConnected();

        void onClosed();

        void onStart(int quality);

        void onStop();

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

    public void setDApp(DApp dApp) {
        mDApp = dApp;
    }

    public void releaseDApp() {
        mDApp = null;
        stop();
    }

    public void setAppManager(AppManager appManager) {
        mAppManager = appManager;
    }

    public void startServer(final int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mConn.startServer(port);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
    }

    public void shutdown() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mConn.shutdown();
            }
        }).start();
    }

    public void onAppLaunch(boolean success) {
        if (success) {
            start();
            mHandler.postDelayed(mConnectTimeoutRunnable, CONNECTED_TIMEOUT);
        } else {
            close(false);
        }
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
        heartbeat();

        if (mDApp == null) {
            close(false);
            return;
        }

        startHeartbeatTimer();
        startHeartbeatTimeout();
    }

    @Override
    public void onClosed(NettyConnection conn) {

        if (mDApp != null) {
            stop();
            DAppApi.clientDisconnected(mSession, mDApp);
        }
        mSession = null;
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
        cause.printStackTrace();

        stop();

        if (mListener != null) {
            mListener.onException(cause);
        }
    }

    private DataServer() {
        mConn = new NettyConnection(this);
        mGson = new Gson();
        mHandler = new Handler();
        mStop = true;
    }

    private void onReceiveHeartbeat() {
        stopHeartbeatTimeout();
        startHeartbeatTimeout();
    }

    private void onReceiveTimestamp(long clientTime) {

        Message pkt = ProtocolPacket.generateTimestamp(clientTime);
        mConn.write(pkt);
    }

    private void onClientMinitouchData(String cmd) {
        Touch.getInstance().sendTouch(cmd);
    }

    private void processProtocolPacket(String protocolJson) {
        Req req = mGson.fromJson(protocolJson, Req.class);
        switch (req.id) {
            case ProtocolPacket.CONNECT_REQ:
                mHandler.removeCallbacks(mConnectTimeoutRunnable);

                final ConnectReq connectReq = mGson.fromJson(protocolJson, ConnectReq.class);
                if (!protocolCompatible(connectReq)) {
                    Message pkt = ProtocolPacket.generateProtocol(ProtocolPacket.CONNECT_RESP, ProtocolPacket.RESULT_NOT_SUPPORT, null);
                    mConn.write(pkt);
                    stop();
                } else if (!verifySession(connectReq)) {
                    stop(); // close client.
                } else if (mAppManager != null
                        && (mAppManager.getState() == AppManager.State.LAUNCHING
                        || mAppManager.getState() == AppManager.State.LAUNCHED)) {
                    mSession = connectReq.data.session;
                    DAppApi.clientConnected(connectReq.data.session, mDApp, new Runnable() {
                        @Override
                        public void run() {
                            onConnectFirstReq(connectReq);
                        }
                    }, new Runnable() {
                        @Override
                        public void run() {
                            stop();
                        }
                    });
                } else {
                    stop();
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
        return req.data != null && !TextUtils.isEmpty(req.data.session);
    }

    private boolean sessionExists() {
        return mSession != null;
    }

    private void onConnectFirstReq(ConnectReq req) {
        mQuality = req.data.quality;

        Message pkt = ProtocolPacket.generateProtocol(ProtocolPacket.CONNECT_RESP, 0, null);
        mConn.write(pkt);
    }

    private void onReceiveDisconnect() {
        Message pkt = ProtocolPacket.generateProtocol(ProtocolPacket.DISCONNECT_RESP, 0, null);
        mConn.write(pkt);

        stop();
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
            mListener.onStart(mQuality);
        }

        mAVDataThread = new SendThread();
        mAVDataThread.start();
        mStop = false;
    }

    private void stop() {
        if (!mStop) {
            mStop = true;

            if (mListener != null) {
                mListener.onStop();
            }
            if (mAVDataThread != null) {
                mAVDataThread.interrupt();
                mAVDataThread.cancel();
                mAVDataThread = null;
            }

            mPacketQueue.clear();
            mConn.closeConnection();

            // fix client terminate with no touch up.
            Touch.getInstance().sendTouch("r\n");

            stopHeartbeatTimer();
            stopHeartbeatTimeout();
            mHandler.removeCallbacks(mConnectTimeoutRunnable);
        }

        if (mAppManager != null) {
            mAppManager.stopApp();
        }
    }

    private void close(boolean stopApp) {
        if (mConn != null) {
            mConn.closeConnection();
        }
        if (mAppManager != null && stopApp) {
            mAppManager.stopApp();
        }
    }

    private Runnable mConnectTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (!sessionExists()) {
                close(true);
            } else {
            }
        }
    };

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
                } catch (Exception e) {
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
            heartbeat();
            mHandler.postDelayed(this, HEARTBEAT_INTERVAL);
        }
    };

    private void heartbeat() {
        Message pkt = ProtocolPacket.generateHeartbeat();
        mConn.write(pkt);
    }

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
}
