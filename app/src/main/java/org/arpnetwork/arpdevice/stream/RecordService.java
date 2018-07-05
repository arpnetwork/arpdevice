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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.Toast;

import org.arpnetwork.arpdevice.MainActivity;
import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.screenrecoder.ScreenRecorder;
import org.arpnetwork.arpdevice.screenrecoder.VideoEncodeConfig;
import org.arpnetwork.arpdevice.server.DataServer;

import static org.arpnetwork.arpdevice.screenrecoder.ScreenRecorder.VIDEO_MIME_TYPE;

public class RecordService extends Service {
    private static final String TAG = RecordService.class.getSimpleName();

    public static final String EXTRA_COMMAND = "command";
    public static final int COMMAND_START = 0;
    public static final int COMMAND_STOP = 1;
    public static final int TYPE_AUDIO = 1;
    public static final int TYPE_VIDEO = 2;
    private static final int id = 0x1fff;
    private static SparseIntArray sQualities = new SparseIntArray(2);

    private NotificationManager mNM;
    private ScreenRecorder mRecorder;
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private Intent mResultData;
    private int mResultCode;
    private int mQuality = 0;

    @Override
    public void onCreate() {
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        sQualities.append(1, 2500 * 1000);
        sQualities.append(2, 3000 * 1000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mResultCode = intent.getIntExtra("code", -1);
        mResultData = intent.getParcelableExtra("data");
        mQuality = intent.getIntExtra("quality", 0);

        int action = intent.getIntExtra(EXTRA_COMMAND, COMMAND_START);
        if (action == COMMAND_START) {
            mMediaProjection = createMediaProjection();
            startRecordIfNeeded();
        } else {
            stopRecord();
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        hideNotification();
    }

    private MediaProjection createMediaProjection() {
        return mMediaProjectionManager.getMediaProjection(mResultCode, mResultData);
    }

    private void startRecordIfNeeded() {
        if (mRecorder != null) return;

        VideoEncodeConfig video = createVideoConfig();
        mRecorder = new ScreenRecorder(video, getResources().getDisplayMetrics().densityDpi, mMediaProjection);
        mRecorder.setCallback(new ScreenRecorder.RecorderCallback() {
            @Override
            public void onStop(Throwable error) {
                hideNotification();
                if (error != null) {
                    Log.e(TAG, "onStop error = " + error.getMessage());
                    Toast.makeText(RecordService.this, "record error,disconnected.", Toast.LENGTH_SHORT).show();
                    DataServer.getInstance().onClientDisconnected();
                }
            }

            @Override
            public void onStart() {
                showNotification("recording...");
            }

            @Override
            public void onRecording(long presentationTimeUs) {
            }

            @Override
            public void onRecordingVideo(long presentationTimeUs, byte[] bytes) {
                DataServer.getInstance().enqueueAVPacket(TYPE_VIDEO, presentationTimeUs, bytes);
            }

            @Override
            public void onRecordingAudio(long presentationTimeUs, byte[] bytes) {
                DataServer.getInstance().enqueueAVPacket(TYPE_AUDIO, presentationTimeUs, bytes);
            }
        });
        mRecorder.start();
    }

    private void stopRecord() {
        if (mRecorder != null) {
            mRecorder.quit();
        }
        mRecorder = null;
    }

    private VideoEncodeConfig createVideoConfig() {
        int width = 720;
        int height = 1280;
        int framerate = 15;
        int iframe = 100;
        int bitrate = sQualities.get(mQuality, 2500 * 1000);
        DataServer.getInstance().onVideoChanged(width, height, mQuality);

        return new VideoEncodeConfig(width, height, bitrate, framerate, iframe, VIDEO_MIME_TYPE);
    }

    private void showNotification(String text) {
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(contentIntent)
                .build();
        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        mNM.notify(id, notification);
    }

    private void hideNotification() {
        mNM.cancel(id);
    }
}