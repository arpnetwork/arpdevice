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

import org.arpnetwork.adb.Connection;
import org.arpnetwork.adb.RawChannel;
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

    private static final int VIDEO_WIDTH = 720;

    private RawChannel mVideoShell;
    private final ByteBuf mBuffer = Unpooled.buffer(1024 * 2);

    public void startRecord(int quality, Connection connection) {
        int height = getVideoHeight();
        DataServer.getInstance().onVideoChanged(VIDEO_WIDTH, height, quality);

        mVideoShell = connection.openExec(getRecordCmd());
        mVideoShell.setListener(new RawChannel.RawListener() {
            @Override
            public void onRaw(RawChannel ch, byte[] data) {
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
                    mBuffer.discardSomeReadBytes();
                } while (mBuffer.readableBytes() > 4);
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
        String cmd = "LD_LIBRARY_PATH=/data/local/tmp /data/local/tmp/arpcap -s %dx%d " +
                "--crop-top %d --crop-bottom %d --preset=ultrafast --bitrate=2048 -p pipe://1";
        Context context = CustomApplication.sInstance;
        return String.format(Locale.US, cmd, VIDEO_WIDTH, getVideoHeight(), UIHelper.getStatusbarHeight(context), UIHelper.getVirtualBarHeight(context));
    }

    private int getVideoHeight() {
        Context context = CustomApplication.sInstance;
        int videoHeight = (int) ((UIHelper.getHeightNoVirtualBar(context) - UIHelper.getStatusbarHeight(context))
                / (double) UIHelper.getWidthNoVirtualBar(context) * VIDEO_WIDTH);
        if (videoHeight % 2 != 0) {
            videoHeight -= 1;
        }
        return videoHeight;
    }
}