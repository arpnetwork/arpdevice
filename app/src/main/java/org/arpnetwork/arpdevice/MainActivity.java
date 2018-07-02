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

package org.arpnetwork.arpdevice;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.arpnetwork.arpdevice.server.DataServer;
import org.arpnetwork.arpdevice.stream.RecordService;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_MEDIA_PROJECTION = 1;
    private MediaProjectionManager mMediaProjectionManager;

    private int mQuality;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMediaProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(MEDIA_PROJECTION_SERVICE);

        DataServer.getInstance().setListener(mConnectionListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, R.string.msg_user_cancelled, Toast.LENGTH_SHORT).show();
                return;
            }

            //TODO add toast to inform user to keep allowed once the user has granted.
            Intent service = new Intent(this, RecordService.class);
            service.putExtra("code", resultCode);
            service.putExtra("data", data);
            service.putExtra("quality", mQuality);
            service.putExtra(RecordService.EXTRA_COMMAND, RecordService.COMMAND_START);
            startService(service);
        }
    }

    private void startCaptureIntent() {
        Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION);
    }

    private void stopRecord() {
        Intent service = new Intent(this, RecordService.class);
        service.putExtra(RecordService.EXTRA_COMMAND, RecordService.COMMAND_STOP);
        startService(service);
    }

    private DataServer.ConnectionListener mConnectionListener = new DataServer.ConnectionListener() {

        @Override
        public void onConnected() {

        }

        @Override
        public void onClosed() {
            MainActivity.this.stopRecord();
        }

        @Override
        public void onRecordStart(int quality) {
            MainActivity.this.startCaptureIntent();
            mQuality = quality;
        }

        @Override
        public void onRecordStop() {
            MainActivity.this.stopRecord();
        }

        @Override
        public void onException(Throwable cause) {

        }
    };
}
