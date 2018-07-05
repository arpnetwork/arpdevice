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
import android.util.Log;

import org.arpnetwork.arpdevice.stream.RecordService;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.data.AVPacket;
import org.arpnetwork.arpdevice.data.ChangeQualityReq;
import org.arpnetwork.arpdevice.data.ConnectReq;
import org.arpnetwork.arpdevice.data.Message;
import org.arpnetwork.arpdevice.data.ProtocolPacket;
import org.arpnetwork.arpdevice.data.Req;
import org.arpnetwork.arpdevice.data.TouchSetting;
import org.arpnetwork.arpdevice.data.VideoInfo;

import java.util.concurrent.LinkedBlockingQueue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class DataServer implements NettyConnection.ConnectionListener {
    private static final String TAG = "DataServer";
    private static final int HEARTBEAT_TIMEOUT = 10000;
    private static final int HEARTBEAT_INTERVAL = 5000;
    private static volatile DataServer sInstance;

    private final LinkedBlockingQueue<AVPacket> mPacketQueue = new LinkedBlockingQueue<AVPacket>();
    private Handler mHandler = new Handler();

    private SendThread mAVDataThread;
    private int mQuality;

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

    public void startServer() {
        Log.d(TAG, "startServer.");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mConn.startServer();
                } catch (InterruptedException e) {
                    // ignored
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

    public void onClientDisconnected() {
        stop();
    }

    /**
     * RecordService startRecord.
     */
    public void onVideoChanged(int width, int height, int quality) {
        VideoInfo videoInfo = new VideoInfo(width, height, quality);
        Message pkt = ProtocolPacket.generateProtocol(ProtocolPacket.VIDEO_CHANGED, 0, videoInfo);
        mConn.write(pkt);
    }

    public void enqueueAVPacket(int type, long presentationTimeUs, byte[] bytes) {
        mPacketQueue.add(new AVPacket(type, presentationTimeUs, bytes, bytes.length));
    }

    @Override
    public void onConnected(NettyConnection conn) {
        startHeartbeatTimer();
        startHeartbeatTimeout();
    }

    @Override
    public void onClosed(NettyConnection conn) {
        stop();
    }

    @Override
    public void onMessage(NettyConnection conn, Message msg) throws Exception {
        switch (msg.type()) {
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
                processProtocolPktBuffer(msg.parsePacket(ConnectReq.class));
                break;

            default:
                break;
        }
    }

    @Override
    public void onException(NettyConnection conn, Throwable cause) {
        Log.e(TAG, "onException.cause = " + cause.getMessage());
    }

    private DataServer() {
        mConn = new NettyConnection(this);
    }

    private void onReceiveHeartbeat() {
        stopHeartbeatTimeout();
        startHeartbeatTimeout();
    }

    private void onReceiveTimestamp(long clientTime) {
        // fixme add time delay with client.
        /*Message pkt = ProtocolPacket.generateTimestamp(clientTime);
        mConn.write(pkt);*/
    }

    private void onClientMinitouchData(String cmd) {
        Touch.getInstance().sendTouch(cmd);
    }

    private void processProtocolPktBuffer(Req protocolPacket) {
        switch (protocolPacket.id) {
            case ProtocolPacket.CONNECT_REQ:
                onConnectFirstReq((ConnectReq) protocolPacket);
                start();
                break;

            case ProtocolPacket.DISCONNECT_REQ:
                onReceiveDisconnect();
                break;

            case ProtocolPacket.CHANGE_QUALITY_REQ:
                onChangeQualityReq((ChangeQualityReq) protocolPacket);
                break;

            default:
                break;
        }
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
        // TODO change quality need sync to execute.

        Message pkt = ProtocolPacket.generateProtocol(ProtocolPacket.CHANGE_QUALITY_RESP, 0, null);
        mConn.write(pkt);
    }

    /**
     * arptouch connect.
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

        mConn.closeConnection();

        // fix client terminate with no touch up.
        Touch.getInstance().sendTouch("r\n");

        this.stopHeartbeatTimer();
        this.stopHeartbeatTimeout();
    }

    private class SendThread extends Thread {
        private boolean mStopped;

        @Override
        public void run() {
            while (!mStopped) {
                try {
                    AVPacket packet = mPacketQueue.take();
                    send(packet);
                } catch (InterruptedException e) {
                    Log.e(TAG, "SendThread interrupted.");
                } catch (Exception e) {
                    Log.e(TAG, "SendThread failed.");
                }
            }
        }

        private void send(AVPacket packet) {
            int bufferSize = 1 + 8 + packet.data.length;

            ByteBuf byteBuf = Unpooled.buffer(bufferSize);
            int avType = packet.type;
            if (avType == RecordService.TYPE_VIDEO) {
                byteBuf.writeByte(Message.VIDEO);
            } else if (avType == RecordService.TYPE_AUDIO) {
                byteBuf.writeByte(Message.AUDIO);
            }
            byteBuf.writeLong(packet.pts);
            byteBuf.writeBytes(packet.data);

            mConn.write(new Message(byteBuf));
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
}
