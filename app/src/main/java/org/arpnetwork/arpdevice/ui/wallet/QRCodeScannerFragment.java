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

package org.arpnetwork.arpdevice.ui.wallet;

import android.content.Intent;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView.OnQRCodeReadListener;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.constant.Constant;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;

public class QRCodeScannerFragment extends BaseFragment implements OnQRCodeReadListener {
    private QRCodeReaderView mQRCodeReaderView;
    private QRCoverView mCoverView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = getBaseActivity().getToolbar();
        toolbar.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        mQRCodeReaderView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mQRCodeReaderView.stopCamera();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qrcode_scanner, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        initViews();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initViews() {
        ImageButton exitBtn = (ImageButton) findViewById(R.id.btn_exit);
        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mQRCodeReaderView = (QRCodeReaderView) findViewById(R.id.qr_decoder_view);
        mQRCodeReaderView.setOnQRCodeReadListener(this);
        mQRCodeReaderView.setQRDecodingEnabled(true);
        mQRCodeReaderView.setTorchEnabled(true);
        mQRCodeReaderView.setBackCamera();

        mCoverView = (QRCoverView) findViewById(R.id.qr_cover_view);
    }

    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        RectF finderRect = mCoverView.getScannerRect();
        boolean isContain = true;
        for (int i = 0, length = points.length; i < length; i++) {
            if (!finderRect.contains(points[i].x, points[i].y)) {
                isContain = false;
                break;
            }
        }
        if (isContain) {
            Intent intent = new Intent();
            intent.putExtra(Constant.ACTIVITY_RESULT_KEY_PRIVATE, text);
            getActivity().setResult(0, intent);
            finish();
        }
    }
}
