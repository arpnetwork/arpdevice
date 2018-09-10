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
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    private LinearLayout mResultView;
    private LinearLayout mProcessView;
    private ImageView mImage;
    private TextView mTitleText;
    private TextView mTipText;
    private TextView mErrorText;
    private Button mTipButton;
    private Button mResetButton;
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

        mCheckThread.quit();
        mCheckThread = null;
    }

    @Override
    protected void setContentView() {
        setContentView(R.layout.activity_check);
    }

    @Override
    protected void initViews() {
        mProcessView = findViewById(R.id.ll_progress);
        mResultView = findViewById(R.id.ll_result);
        mImage = findViewById(R.id.iv_info);
        mProgressbar = findViewById(R.id.progressbar);
        mTitleText = findViewById(R.id.tv_title);
        mTipText = findViewById(R.id.tv_tip);
        mErrorText = findViewById(R.id.tv_reason);
        mTipButton = findViewById(R.id.btn_tip);
        mTipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mCheckCode) {
                    case Constant.CHECK_OS:
                    case Constant.CHECK_DISK_AVAILABLE:
                    case Constant.CHECK_TOUCH:
                        finish();
                        getApplication().onTerminate();
                        break;

                    case Constant.CHECK_ADB:
                    case Constant.CHECK_ADB_SAFE:
                    case Constant.CHECK_INSTALLATION_FAILED:
                        jumpToSettingADB();
                        break;

                    // TODO: check UPNP again
                    case Constant.CHECK_UPNP:
                        break;

                    default:
                        break;
                }
            }
        });

        mResetButton = findViewById(R.id.btn_progress);
        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Touch.getInstance().ensureAuthChecked(mUIHandler);
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
        Intent intent = new Intent();
        if (!Wallet.exists()) {
            intent.putExtra(Constant.KEY_FROM_LAUNCHER, true);
            intent.setClass(this, WalletImporterActivity.class);
        } else {
            intent.setClass(this, MyActivity.class);
        }
        startActivity(intent);
        finish();
    }

    @Override
    public boolean handleMessage(Message msg) {
        mProcessView.setVisibility(View.GONE);
        mResultView.setVisibility(View.VISIBLE);
        mProgressbar.setVisibility(View.GONE);
        mTipButton.setVisibility(View.VISIBLE);
        mImage.setVisibility(View.VISIBLE);
        mErrorText.setVisibility(View.VISIBLE);
        SpannableStringBuilder highlightText;
        mCheckCode = msg.what;
        switch (msg.what) {
            case Constant.CHECK_OS:
                mTitleText.setText(R.string.check_init);
                mTipText.setText(R.string.check_device_error);
                mTipButton.setText(R.string.check_btn_quit);
                mImage.setImageResource(R.mipmap.check_failed);
                mErrorText.setText(String.format("%s\n%s", getString(R.string.check_failed_reason), getString(R.string.check_fail_os)));
                break;

            case Constant.CHECK_DISK_AVAILABLE:
                mTitleText.setText(R.string.check_init);
                mTipText.setText(R.string.check_device_error);
                mTipButton.setText(R.string.check_btn_quit);
                mImage.setImageResource(R.mipmap.check_failed);
                mErrorText.setText(String.format("%s\n%s", getString(R.string.check_failed_reason), getString(R.string.check_fail_disk)));
                break;

            case Constant.CHECK_ADB:
                mTitleText.setText(R.string.check_USB);
                highlightText = createHighlight(getString(R.string.check_fail_usb),
                        getString(R.string.check_highlight_developer_options), getString(R.string.check_highlight_USB_debug));
                mTipText.setText(highlightText);
                mTipButton.setText(R.string.check_btn_adb);
                mImage.setImageResource(R.mipmap.check_usb);
                mErrorText.setVisibility(View.GONE);
                break;

            case Constant.CHECK_TCP:
                mTitleText.setText(R.string.check_tcp);
                mTipText.setText(R.string.check_fail_tcp);
                mTipButton.setVisibility(View.GONE);
                mImage.setImageResource(R.mipmap.check_failed);
                mErrorText.setText(R.string.download_tcp_tool);
                break;

            case Constant.CHECK_AUTH:
                mTitleText.setText(R.string.check_authorization);
                mProcessView.setVisibility(View.VISIBLE);
                mResultView.setVisibility(View.GONE);
                findViewById(R.id.pb_progress).setVisibility(View.GONE);
                // TODO: get RSA key
                String RSAKey = "DE:2F:EA:0D:12:2E:B4:4F:D8:EE:83:87:7C:2B:6D:66";
                highlightText = createHighlight(getString(R.string.check_authorization_tip), RSAKey,
                        getString(R.string.check_always_allow));
                setProcessTip(highlightText, View.TEXT_ALIGNMENT_TEXT_START);
                mResetButton.setVisibility(View.VISIBLE);
                break;

            // TODO: Add check touch in CheckThread
            case Constant.CHECK_TOUCH:
                mTitleText.setText(R.string.check_touch);
                mTipText.setText(R.string.check_fail_touch);
                mTipButton.setText(R.string.check_btn_quit);
                mImage.setImageResource(R.mipmap.check_failed);
                mErrorText.setVisibility(View.GONE);
                break;

            case Constant.CHECK_ADB_SAFE:
                mCheckThread.setShouldPing(true);

                mTitleText.setText(R.string.check_touch_safe);
                highlightText = createHighlight(getString(R.string.check_fail_adb_safe),
                        getString(R.string.check_highlight_safe));
                mTipText.setText(highlightText);
                mTipButton.setText(R.string.check_btn_adb);
                mImage.setImageResource(R.mipmap.check_adb_safe);
                mErrorText.setVisibility(View.GONE);
                break;

            // TODO: Add check installation in CheckThread
            case Constant.CHECK_INSTALLATION:
                mProcessView.setVisibility(View.VISIBLE);
                mResultView.setVisibility(View.GONE);
                mTitleText.setText(R.string.check_installation);
                findViewById(R.id.pb_progress).setVisibility(View.VISIBLE);
                highlightText = createHighlight(getString(R.string.check_installation_tip),
                        getString(R.string.check_highlight_installation));
                setProcessTip(highlightText, View.TEXT_ALIGNMENT_CENTER);
                mResetButton.setVisibility(View.GONE);
                break;

            // TODO: Add check installation failed in CheckThread
            case Constant.CHECK_INSTALLATION_FAILED:
                mTitleText.setText(R.string.check_installation);
                highlightText = createHighlight(getString(R.string.check_fail_installation),
                        getString(R.string.check_highlight_install));
                mTipText.setText(highlightText);
                mTipButton.setText(R.string.check_btn_adb);
                mImage.setImageResource(R.mipmap.check_usb_installation);
                mErrorText.setVisibility(View.GONE);
                break;

            // TODO: Add check UPNP in CheckThread
            case Constant.CHECK_UPNP:
                mTitleText.setText(R.string.check_network);
                mTipText.setText(R.string.check_network_error);
                mTipButton.setText(R.string.check_again);
                mImage.setImageResource(R.mipmap.check_failed);
                mErrorText.setVisibility(View.GONE);
                break;

            case Constant.CHECK_AUTH_SUCCESS:
                mTitleText.setText(R.string.check_success);
                mImage.setImageResource(R.mipmap.check_success);
                mTipButton.setVisibility(View.GONE);
                mErrorText.setVisibility(View.GONE);
                mTipText.setText(R.string.check_success_tip);

                mUIHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        jumpToNextActivity();
                    }
                }, 500);
                break;

            default:
                break;
        }
        return false;
    }

    private SpannableStringBuilder createHighlight(String formatText, String first, String second) {
        String allText = String.format(formatText, first, second);
        SpannableStringBuilder style = new SpannableStringBuilder(allText);
        highlight(style, first);
        highlight(style, second);
        return style;
    }

    private SpannableStringBuilder createHighlight(String formatText, String highlight) {
        String allText = String.format(formatText, highlight);
        SpannableStringBuilder style = new SpannableStringBuilder(allText);
        return highlight(style, highlight);
    }

    private SpannableStringBuilder highlight(SpannableStringBuilder style, String highlight) {
        int startIndex = style.toString().indexOf(highlight);
        if (startIndex >= 0) {
            style.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)),
                    startIndex, startIndex + highlight.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        }
        return style;
    }

    private void setProcessTip(SpannableStringBuilder text, int textAlignment) {
        TextView topTip = findViewById(R.id.tv_tip_top);
        topTip.setText(text);
        topTip.setTextAlignment(textAlignment);
        TextView middleTip = findViewById(R.id.tv_tip_middle);
        middleTip.setText(text);
        middleTip.setTextAlignment(textAlignment);
        TextView bottomTip = findViewById(R.id.tv_tip_bottom);
        bottomTip.setText(text);
        bottomTip.setTextAlignment(textAlignment);
    }
}