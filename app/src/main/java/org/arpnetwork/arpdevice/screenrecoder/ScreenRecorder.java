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

package org.arpnetwork.arpdevice.screenrecoder;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenRecorder {
    private static final String TAG = "ScreenRecorder";
    private static final boolean VERBOSE = false;

    public static final String VIDEO_MIME_TYPE = "video/avc"; // H.264

    private int mWidth;
    private int mHeight;
    private int mDpi;
    private MediaProjection mMediaProjection;
    private VideoEncoder mVideoEncoder;

    private MediaFormat mVideoOutputFormat = null;
    private boolean mExtractStarted = false;

    private AtomicBoolean mForceQuit = new AtomicBoolean(false);
    private AtomicBoolean mIsRunning = new AtomicBoolean(false);
    private VirtualDisplay mVirtualDisplay;
    private MediaProjection.Callback mProjectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            quit();
        }
    };

    private HandlerThread mWorker;
    private CallbackHandler mHandler;

    private RecorderCallback mCallback;
    private LinkedList<Integer> mPendingVideoEncoderBufferIndices = new LinkedList<>();
    private LinkedList<MediaCodec.BufferInfo> mPendingVideoEncoderBufferInfos = new LinkedList<>();

    public interface RecorderCallback {
        void onStop(Throwable error);

        void onStart();

        void onRecording(long presentationTimeUs);

        void onRecordingVideo(long presentationTimeUs, byte[] bytes);

        void onRecordingAudio(long presentationTimeUs, byte[] bytes);
    }

    public ScreenRecorder(VideoEncodeConfig video, int dpi, MediaProjection mp) {
        mWidth = video.width;
        mHeight = video.height;
        mDpi = dpi;
        mMediaProjection = mp;
        mVideoEncoder = new VideoEncoder(video);
    }

    public void start() {
        if (mWorker != null) throw new IllegalStateException();
        mWorker = new HandlerThread(TAG);
        mWorker.start();
        mHandler = new CallbackHandler(mWorker.getLooper());
        mHandler.sendEmptyMessage(MSG_START);
    }

    public final void quit() {
        mForceQuit.set(true);
        if (!mIsRunning.get()) {
            release();
        } else {
            signalStop(false);
        }

    }

    public void setCallback(RecorderCallback callback) {
        mCallback = callback;
    }

    private void signalEndOfStream() {
        MediaCodec.BufferInfo eos = new MediaCodec.BufferInfo();
        ByteBuffer buffer = ByteBuffer.allocate(0);
        eos.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

        writeSampleData(eos, buffer);
    }

    private void record() {
        if (mIsRunning.get() || mForceQuit.get()) {
            throw new IllegalStateException();
        }
        if (mMediaProjection == null) {
            throw new IllegalStateException("maybe release");
        }
        mIsRunning.set(true);

        mMediaProjection.registerCallback(mProjectionCallback, mHandler);

        try {
            prepareVideoEncoder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-display",
                mWidth, mHeight, mDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                mVideoEncoder.getInputSurface(), null, null);
    }

    private void extractVideo(int index, MediaCodec.BufferInfo bufferInfo) {
        if (!mIsRunning.get()) {
            Log.w(TAG, "extractVideo: Already stopped!");
            return;
        }
        if (!mExtractStarted) {
            mPendingVideoEncoderBufferIndices.add(index);
            mPendingVideoEncoderBufferInfos.add(bufferInfo);
            return;
        }
        ByteBuffer encodedData = mVideoEncoder.getOutputBuffer(index);
        writeSampleData(bufferInfo, encodedData);
        mVideoEncoder.releaseOutputBuffer(index);
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            // send release msg
            signalStop(true);
        }
    }

    private void writeSampleData(MediaCodec.BufferInfo bufferInfo, ByteBuffer encodedData) {
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            bufferInfo.size = 0;
        }
        boolean eos = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
        if (bufferInfo.size == 0 && !eos) {
            encodedData = null;
        } else {
            if (!eos && mCallback != null) {
                mCallback.onRecording(bufferInfo.presentationTimeUs);
            }
        }
        if (encodedData != null) {
            encodedData.position(bufferInfo.offset);
            encodedData.limit(bufferInfo.offset + bufferInfo.size);
            if (mCallback != null) {
                byte[] bytes = new byte[encodedData.remaining()];
                encodedData.get(bytes);
                mCallback.onRecordingVideo(bufferInfo.presentationTimeUs, bytes);
            }
        }
    }

    private void resetVideoOutputFormat(MediaFormat newFormat) {
        // should happen before receiving buffers, and should only happen once
        if (mExtractStarted) {
            throw new IllegalStateException("output format already changed!");
        }
        if (VERBOSE) {
            Log.i(TAG, "Video output format changed.\n New format: " + newFormat.toString());
        }
        mVideoOutputFormat = newFormat;
        if (mCallback != null) {
            ByteBuffer sps = newFormat.getByteBuffer("csd-0"); // sps
            byte[] bytes = new byte[sps.remaining()];
            sps.get(bytes);
            mCallback.onRecordingVideo(0, bytes);

            ByteBuffer pps = newFormat.getByteBuffer("csd-1"); // pps
            bytes = new byte[pps.remaining()];
            pps.get(bytes);
            mCallback.onRecordingVideo(0, bytes);
        }
    }

    private void startExtractIfReady() {
        if (mExtractStarted || mVideoOutputFormat == null) {
            return;
        }

        mExtractStarted = true;
        if (VERBOSE) {
            Log.i(TAG, "Started media extract");
        }
        if (mPendingVideoEncoderBufferIndices.isEmpty()) {
            return;
        }
        if (VERBOSE) {
            Log.i(TAG, "extract pending video output buffers...");
        }
        MediaCodec.BufferInfo info;
        while ((info = mPendingVideoEncoderBufferInfos.poll()) != null) {
            int index = mPendingVideoEncoderBufferIndices.poll();
            extractVideo(index, info);
        }
        if (VERBOSE) {
            Log.i(TAG, "extract pending video output buffers done.");
        }
    }

    private void prepareVideoEncoder() throws IOException {
        VideoEncoder.Callback callback = new VideoEncoder.Callback() {
            @Override
            public void onOutputBufferAvailable(BaseEncoder codec, int index, MediaCodec.BufferInfo info) {
                extractVideo(index, info);
            }

            @Override
            public void onError(Encoder codec, Exception e) {
                Log.e(TAG, "VideoEncoder ran into an error! ", e);
                Message.obtain(mHandler, MSG_ERROR, e).sendToTarget();
            }

            @Override
            public void onOutputFormatChanged(BaseEncoder codec, MediaFormat format) {
                resetVideoOutputFormat(format);
                startExtractIfReady();
            }
        };
        mVideoEncoder.setCallback(callback);
        mVideoEncoder.prepare();
    }

    private void signalStop(boolean stopWithEOS) {
        Message msg = Message.obtain(mHandler, MSG_STOP, stopWithEOS ? STOP_WITH_EOS : 0, 0);
        mHandler.sendMessageAtFrontOfQueue(msg);
    }

    private void stopEncoders() {
        mIsRunning.set(false);
        mPendingVideoEncoderBufferIndices.clear();
        try {
            if (mVideoEncoder != null) mVideoEncoder.stop();
        } catch (IllegalStateException e) {
            // ignored
        }
    }

    private void release() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mProjectionCallback);
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        mVideoOutputFormat = null;
        mExtractStarted = false;

        if (mWorker != null) {
            mWorker.quitSafely();
            mWorker = null;
        }
        if (mVideoEncoder != null) {
            mVideoEncoder.release();
            mVideoEncoder = null;
        }

        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        mHandler = null;
    }

    private static final int MSG_START = 0;
    private static final int MSG_STOP = 1;
    private static final int MSG_ERROR = 2;
    private static final int STOP_WITH_EOS = 1;

    private class CallbackHandler extends Handler {
        CallbackHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START:
                    try {
                        record();
                        if (mCallback != null) {
                            mCallback.onStart();
                        }
                        break;

                    } catch (Exception e) {
                        msg.obj = e;
                        // exception goto MSG_STOP.
                    }
                case MSG_STOP:
                case MSG_ERROR:
                    stopEncoders();
                    if (msg.arg1 != STOP_WITH_EOS) signalEndOfStream();
                    if (mCallback != null) {
                        mCallback.onStop((Throwable) msg.obj);
                    }
                    release();
                    break;

                default:
                    break;
            }
        }
    }
}
