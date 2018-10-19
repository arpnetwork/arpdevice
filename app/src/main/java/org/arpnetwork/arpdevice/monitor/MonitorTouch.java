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

package org.arpnetwork.arpdevice.monitor;

import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import org.arpnetwork.adb.RawChannel;
import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.constant.Constant;
import org.arpnetwork.arpdevice.stream.Touch;
import org.web3j.utils.Numeric;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MonitorTouch {
    private static final String TAG = "MonitorTouch";
    private static final int MONITOR_DURATION = 1000; // ms

    private final LinkedBlockingQueue<String> mRemotePacketQueue = new LinkedBlockingQueue<String>();
    private final ConcurrentLinkedQueue<String> mCmdQueue = new ConcurrentLinkedQueue<String>();
    private final LinkedBlockingQueue<String> mLocalPacketQueue = new LinkedBlockingQueue<String>();
    private final ConcurrentLinkedQueue<String> mLocalOutQueue = new ConcurrentLinkedQueue<String>();

    private RemoteParseThread mRemoteParseThread;
    private LocalParseThread mLocalParseThread;
    private RawChannel mGeteventChannel;

    private HandlerThread mWorker;
    private Handler mHandler;
    private long mRemoteCommits = 0;
    private long mTimerCount = 0;
    private int mNeedDeleteCount;

    public void startMonitor(String monitorPath) {
        if (TextUtils.isEmpty(monitorPath)) return;

        stopMonitor();
        mRemoteParseThread = new RemoteParseThread();
        mRemoteParseThread.start();
        mLocalParseThread = new LocalParseThread();
        mLocalParseThread.start();

        mWorker = new HandlerThread(TAG);
        mWorker.start();

        mHandler = new Handler(mWorker.getLooper());
        mHandler.postDelayed(mMonitorRunnable, MONITOR_DURATION);

        mGeteventChannel = Touch.getInstance().getConnection().openExec("getevent -l");
        mGeteventChannel.setListener(new RawChannel.RawListener() {
            @Override
            public void onRaw(RawChannel ch, byte[] data) {
                mLocalPacketQueue.add(new String(data));
            }
        });
    }

    public void stopMonitor() {
        mRemoteCommits = 0;
        mTimerCount = 0;
        if (mRemoteParseThread != null) {
            mRemoteParseThread.cancel();
            mRemoteParseThread.interrupt();
        }
        if (mLocalParseThread != null) {
            mLocalParseThread.cancel();
            mLocalParseThread.interrupt();
        }

        if (mGeteventChannel != null) {
            mGeteventChannel.close();
            mGeteventChannel = null;
        }
        if (mWorker != null) {
            mWorker.quit();
            mWorker = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    public void enqueueTouch(String touch) {
        mRemotePacketQueue.add(touch);
    }

    private Runnable mMonitorRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (mCmdQueue) {
                if (mCmdQueue.containsAll(mLocalOutQueue)) {
                    if (mTimerCount == 0 && mRemoteCommits >= mTimerCount) {
                        mTimerCount++;
                        mNeedDeleteCount = mCmdQueue.size();
                    } else if (mTimerCount != 0 && mRemoteCommits >= mTimerCount) {
                        mTimerCount++;
                        int m = mCmdQueue.size();

                        for (int i = 0; i < mNeedDeleteCount; i++) {
                            mCmdQueue.poll();
                        }
                        mNeedDeleteCount = m - mNeedDeleteCount;
                    }
                    mLocalOutQueue.clear();
                } else {
                    Log.e(TAG, "abnormal");
                    stopMonitor();
                    sendBroadcast();
                }

                mHandler.postDelayed(this, MONITOR_DURATION);
            }
        }
    };

    private void sendBroadcast() {
        Intent localIntent = new Intent();
        localIntent.setAction(Constant.BROADCAST_ACTION_TOUCH_LOCAL);
        LocalBroadcastManager.getInstance(CustomApplication.sInstance).sendBroadcast(localIntent);
    }

    private class RemoteParseThread extends Thread {
        private boolean mStopped;

        @Override
        public void run() {
            mRemotePacketQueue.clear();
            mCmdQueue.clear();

            while (!mStopped) {
                try {
                    String packet = mRemotePacketQueue.take();
                    if (!TextUtils.isEmpty(packet)) {
                        int packetCount = addPacket(packet);
                        if (packetCount > 0) {
                            mRemoteCommits++;
                        }
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "RemoteParseThread interrupted.");
                } catch (Exception e) {
                    Log.e(TAG, "RemoteParseThread failed.");
                }
            }
        }

        private int addPacket(String packet) throws IOException {
            int matchCount = 0;
            BufferedReader br = new BufferedReader(new StringReader(packet));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("d") || line.contains("m")) {
                    String positionXDec = line.split(" ")[2];
                    mCmdQueue.add(positionXDec);
                    matchCount++;
                }
            }

            return matchCount;
        }

        private void cancel() {
            mStopped = true;
        }
    }

    private class LocalParseThread extends Thread {
        private static final String ABS_POS_X = "ABS_MT_POSITION_X";
        private static final String TARGET = "EV_ABS       ABS_MT_POSITION_X";
        private static final String KEY_MENU = "KEY_MENU";
        private static final String KEY_HOME = "KEY_HOME";
        private static final String KEY_BACK = "KEY_BACK";
        private static final String KEY_VOLUMEDOWN = "KEY_VOLUMEDOWN";
        private static final String KEY_VOLUMEUP = "KEY_VOLUMEUP";
        private final int ABS_POS_X_LEN = "ABS_MT_POSITION_X".length();
        private boolean mStopped;

        @Override
        public void run() {
            mLocalPacketQueue.clear();
            mLocalOutQueue.clear();

            while (!mStopped) {
                try {
                    String packet = mLocalPacketQueue.take();
                    parsePacket(packet);
                } catch (InterruptedException e) {
                    Log.e(TAG, "LocalParseThread interrupted.");
                } catch (Exception e) {
                    Log.e(TAG, "LocalParseThread failed.");
                }
            }
        }

        private void cancel() {
            mStopped = true;
        }

        private int parsePacket(String packet) throws IOException {
            // parse line:  EV_SYN       ABS_MT_POSITION_X
            int matchCount = 0;
            BufferedReader br = new BufferedReader(new StringReader(packet));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(TARGET)) {
                    String posX = line.substring(line.indexOf(ABS_POS_X) + ABS_POS_X_LEN).trim();
                    String positionXDec = Numeric.toBigInt(posX).toString(10);
                    mLocalOutQueue.add(positionXDec);
                    matchCount++;
                }
                if (line.contains(KEY_MENU) || line.contains(KEY_HOME) || line.contains(KEY_BACK)
                        || line.contains(KEY_VOLUMEDOWN) || line.contains(KEY_VOLUMEUP)) {
                    stopMonitor();
                    sendBroadcast();
                    Log.e(TAG, "key abnormal");
                    break;
                }
            }
            return matchCount;
        }
    }
}
