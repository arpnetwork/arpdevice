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

package org.arpnetwork.arpdevice.stream;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import org.arpnetwork.adb.RawChannel;
import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.config.Constant;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.LinkedBlockingQueue;

public class MonitorTouch {
    private static final String TAG = "MonitorTouch";

    private long mRemoteCommits = 0;
    private final LinkedBlockingQueue<String> mPacketQueue = new LinkedBlockingQueue<String>();
    private ParseThread mParseThread;

    public void startMonitor(String monitorPath) {
        if (TextUtils.isEmpty(monitorPath)) return;

        stopMonitor();
        mParseThread = new ParseThread();
        mParseThread.start();

        RawChannel ss = Touch.getInstance().getConnection().openExec("getevent -l " + monitorPath);
        ss.setListener(new RawChannel.RawListener() {
            @Override
            public void onRaw(RawChannel ch, byte[] data) {
                mPacketQueue.add(new String(data));
            }
        });
    }

    public void stopMonitor() {
        mRemoteCommits = 0;
        if (mParseThread != null) {
            mParseThread.cancel();
            mParseThread.interrupt();
        }
    }

    public void increaseCount() {
        mRemoteCommits++;
    }

    private class ParseThread extends Thread {
        private boolean mStopped;
        private long commits;

        @Override
        public void run() {
            mPacketQueue.clear();

            while (!mStopped) {
                try {
                    String packet = mPacketQueue.take();
                    handleMonitor(packet);
                } catch (InterruptedException e) {
                    Log.e(TAG, "ParseThread interrupted.");
                } catch (Exception e) {
                    Log.e(TAG, "ParseThread failed.");
                }
            }
        }

        private void handleMonitor(String packet) {
            // parse line:  EV_SYN       SYN_REPORT           00000000
            try {
                commits = parsePacket(packet);
                if (commits > mRemoteCommits) {
                    Log.e(TAG, "abnormal");
                    sendBroadcast();
                }
            } catch (IOException ignored) {
            }
        }

        private void sendBroadcast() {
            Intent localIntent = new Intent();
            localIntent.setAction(Constant.BROADCAST_ACTION_TOUCH_LOCAL);
            LocalBroadcastManager.getInstance(CustomApplication.sInstance).sendBroadcast(localIntent);
        }

        private void cancel() {
            mStopped = true;
        }

        private int parsePacket(String packet) throws IOException {
            int matchCount = 0;
            BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(packet.getBytes(Charset.forName("utf8"))), Charset.forName("utf8")));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("EV_SYN       SYN_REPORT")) {
                    matchCount++;
                }
            }
            return matchCount;
        }
    }
}
