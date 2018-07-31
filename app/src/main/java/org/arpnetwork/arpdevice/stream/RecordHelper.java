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

import android.content.Context;
import android.util.Log;

import org.arpnetwork.adb.Connection;
import org.arpnetwork.adb.ShellChannel;
import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.data.Message;
import org.arpnetwork.arpdevice.server.DataServer;
import org.arpnetwork.arpdevice.util.UIHelper;

import java.util.Locale;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class RecordHelper {
    private static final String TAG = "RecordHelper";
    private static final boolean DEBUG = Config.DEBUG;

    private ShellChannel mVideoShell;
    private final ByteBuf mBuffer = Unpooled.buffer(1024 * 2);

    public void startRecord(int quality, Connection connection) {
        int width = 720;
        int height = getVideoHeight();
        DataServer.getInstance().onVideoChanged(width, height, quality);

        mVideoShell = connection.openShell(getRecordCmd());
        mVideoShell.setListener(new ShellChannel.ShellListener() {
            public void onStdout(ShellChannel ch, byte[] data) {
                // Continue callback data format: int size + video data.
                mBuffer.writeBytes(data);

                do {
                    mBuffer.markReaderIndex();
                    int size = mBuffer.readIntLE();
                    if (mBuffer.readableBytes() < size) {
                        mBuffer.resetReaderIndex();
                        break;
                    }

                    ByteBuf videoData = mBuffer.readBytes(size);
                    ByteBuf byteBuf = Unpooled.buffer(1 + 8 + size);
                    byteBuf.writeByte(Message.VIDEO);
                    byteBuf.writeLong(System.currentTimeMillis());
                    byteBuf.writeBytes(videoData);

                    DataServer.getInstance().enqueueAVPacket(byteBuf);
                    mBuffer.discardReadBytes();

                } while (mBuffer.readableBytes() > 4);
            }

            @Override
            public void onStderr(ShellChannel ch, byte[] data) {
                Log.e(TAG, "av STDERR:" + new String(data));
            }

            @Override
            public void onExit(ShellChannel ch, int code) {
                Log.e(TAG, "av onExit:" + code);
                mVideoShell = null;
                DataServer.getInstance().onClientDisconnected();
            }
        });
    }

    public void stopRecord() {
        if (mVideoShell != null) {
            mVideoShell.close();
            mVideoShell = null;
        }
    }

    public boolean isRecording() {
        return mVideoShell != null;
    }

    private String getRecordCmd() {
        String cmd = "LD_LIBRARY_PATH=/data/local/tmp /data/local/tmp/arpcap -s 720x%d --crop-top %d " +
                "--crop-bottom %d -p pipe://1";
        Context context = CustomApplication.sInstance;
        return String.format(Locale.US, cmd, getVideoHeight(), UIHelper.getStatusbarHeight(context), UIHelper.getVirtualBarHeight(context));
    }

    private int getVideoHeight() {
        Context context = CustomApplication.sInstance;
        int videoHeight = (int) ((UIHelper.getHeightNoVirtualBar(context) - UIHelper.getStatusbarHeight(context))
                / (double) UIHelper.getWidthNoVirtualBar(context) * 720);
        return videoHeight;
    }
}