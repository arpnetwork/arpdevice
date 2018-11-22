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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.arpnetwork.adb.RawChannel;
import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.constant.Constant;
import org.arpnetwork.arpdevice.stream.Touch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MonitorTouch {
    private static final String TAG = "MonitorTouch";
    private final LinkedBlockingQueue<String> mLocalPacketQueue = new LinkedBlockingQueue<String>();

    private LocalParseThread mLocalParseThread;
    private RawChannel mGeteventChannel;

    public void startMonitor() {
        stopMonitor();

        mLocalParseThread = new LocalParseThread();
        mLocalParseThread.start();

        mGeteventChannel = Touch.getInstance().getConnection().openExec("getevent -l");
        mGeteventChannel.setListener(new RawChannel.RawListener() {
            @Override
            public void onRaw(RawChannel ch, byte[] data) {
                mLocalPacketQueue.add(new String(data));
            }
        });
    }

    public void stopMonitor() {
        if (mLocalParseThread != null) {
            mLocalParseThread.cancel();
            mLocalParseThread.interrupt();
        }

        if (mGeteventChannel != null) {
            mGeteventChannel.close();
            mGeteventChannel = null;
        }
    }

    public static void sendBroadcast() {
        Intent localIntent = new Intent();
        localIntent.setAction(Constant.BROADCAST_ACTION_TOUCH_LOCAL);
        LocalBroadcastManager.getInstance(CustomApplication.sInstance).sendBroadcast(localIntent);
    }

    private class LocalParseThread extends Thread {
        private static final String KEY_MENU = "KEY_MENU";
        private static final String KEY_HOME = "KEY_HOME";
        private static final String KEY_BACK = "KEY_BACK";
        private static final String KEY_VOLUMEDOWN = "KEY_VOLUMEDOWN";
        private static final String KEY_VOLUMEUP = "KEY_VOLUMEUP";
        private boolean mStopped;

        @Override
        public void run() {
            mLocalPacketQueue.clear();

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
                if (line.contains(KEY_MENU) || line.contains(KEY_HOME) || line.contains(KEY_BACK)
                        || line.contains(KEY_VOLUMEDOWN) || line.contains(KEY_VOLUMEUP)) {
                    stopMonitor();
                    MonitorTouch.sendBroadcast();
                    Log.e(TAG, "key abnormal");
                    break;
                }
            }
            return matchCount;
        }
    }
}
