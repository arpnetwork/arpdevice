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

package org.arpnetwork.arpdevice.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.config.Constant;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.ui.base.BaseActivity;
import org.arpnetwork.arpdevice.ui.my.MyActivity;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.ui.wallet.WalletImporterActivity;
import org.arpnetwork.arpdevice.util.UIHelper;

public class CheckDeviceActivity extends BaseActivity implements Handler.Callback {
    private int mCheckCode = Constant.CHECK_DEFAULT;

    private CheckThread mCheckThread;
    private Handler mUIHandler;

    private TextView mTipText;
    private Button mTipButton;
    private ProgressBar mProgressbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUIHandler = new Handler(this);
        mCheckThread = new CheckThread(this, mUIHandler);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mCheckThread.doCheck();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mCheckThread.stopPingTimer();
        mCheckThread.quit();
    }

    @Override
    protected void setContentView() {
        setContentView(R.layout.activity_check);
    }

    @Override
    protected void initViews() {
        mProgressbar = findViewById(R.id.progressbar);
        mTipText = findViewById(R.id.tv_tip);
        mTipButton = findViewById(R.id.btn_tip);
        mTipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mCheckCode) {
                    case Constant.CHECK_OS:
                        finish();
                        getApplication().onTerminate();
                        break;

                    case Constant.CHECK_ADB:
                    case Constant.CHECK_ADB_SAFE:
                        jumpToSettingADB();
                        break;

                    case Constant.CHECK_AUTH:
                        Touch.getInstance().ensureAuthChecked(mUIHandler);
                        break;

                    default:
                        break;
                }
            }
        });
    }

    private void jumpToSettingADB() {
        try {
            ComponentName componentName = new ComponentName("com.android.settings", "com.android.settings.DevelopmentSettings");
            Intent intent = new Intent();
            intent.setComponent(componentName);
            intent.setAction("android.intent.action.View");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            UIHelper.showToast(CheckDeviceActivity.this, getString(R.string.check_fail_adb_exception));
        }
    }

    private void jumpToNextActivity() {
        Touch.getInstance().connect();

        Intent intent = new Intent();
        if (!Wallet.exists()) {
            intent.setClass(this, WalletImporterActivity.class);
        } else {
            intent.setClass(this, MyActivity.class);
        }
        startActivity(intent);
        finish();
    }

    @Override
    public boolean handleMessage(Message msg) {
        mProgressbar.setVisibility(View.GONE);
        mTipButton.setVisibility(View.VISIBLE);

        mCheckCode = msg.what;
        switch (msg.what) {
            case Constant.CHECK_OS:
                mTipText.setText(R.string.check_fail_os);
                mTipButton.setText(R.string.check_btn_quit);
                break;

            case Constant.CHECK_ADB:
                mTipText.setText(R.string.check_fail_adb);
                mTipButton.setText(R.string.check_btn);
                break;

            case Constant.CHECK_TCP:
                mTipText.setText(R.string.check_fail_tcp);
                mTipButton.setVisibility(View.GONE);
                break;

            case Constant.CHECK_AUTH:
                SpannableStringBuilder highlight = createHighlight(getString(R.string.check_fail_auth),
                        getString(R.string.check_highlight));
                mTipText.setText(highlight);
                mTipButton.setText(R.string.check_btn);
                break;

            case Constant.CHECK_ADB_SAFE:
                mCheckThread.setShouldPing(true);

                SpannableStringBuilder highlightSafe = createHighlight(getString(R.string.check_fail_adb_safe),
                        getString(R.string.check_highlight_safe));
                mTipText.setText(highlightSafe);
                mTipButton.setText(R.string.check_btn);
                break;

            case Constant.CHECK_AUTH_SUCCESS:
                mTipButton.setVisibility(View.GONE);
                mTipText.setText(R.string.check_success);

                jumpToNextActivity();
                break;

            default:
                break;
        }
        return false;
    }

    private SpannableStringBuilder createHighlight(String formatText, String highlight) {
        String allText = String.format(formatText, highlight);
        SpannableStringBuilder style = new SpannableStringBuilder(allText);
        int startIndex = allText.indexOf(highlight);
        if (startIndex >= 0) {
            style.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)),
                    startIndex, startIndex + highlight.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        }
        return style;
    }
}