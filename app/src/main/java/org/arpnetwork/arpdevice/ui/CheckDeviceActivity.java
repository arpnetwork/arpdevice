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

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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

import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.constant.Constant;
import org.arpnetwork.arpdevice.data.DeviceInfo;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.ui.base.BaseActivity;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.home.HomeActivity;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.ui.wallet.WalletImporterActivity;
import org.arpnetwork.arpdevice.upnp.ClingRegistryListener;
import org.arpnetwork.arpdevice.util.NetworkHelper;
import org.arpnetwork.arpdevice.util.PreferenceManager;
import org.arpnetwork.arpdevice.util.Util;
import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.android.FixedAndroidLogHandler;

import java.lang.ref.WeakReference;

public class CheckDeviceActivity extends BaseActivity {
    private static final String TAG = "CheckDeviceActivity";

    private int mCheckCode = Constant.CHECK_DEFAULT;

    private LinearLayout mResultView;
    private LinearLayout mProcessView;
    private ImageView mImage;
    private TextView mTitleText;
    private TextView mTipText;
    private TextView mErrorText;
    private Button mTipButton;
    private Button mResetButton;
    private ProgressBar mProgressbar;

    private CheckThread mCheckThread;
    private LocalHandler mUIHandler;

    private AndroidUpnpService upnpService;
    private ClingRegistryListener mClingRegistryListener;
    private int mDataPort;
    private int mHttpPort;

    private boolean mFromMy;
    private Miner mMiner;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            upnpService = (AndroidUpnpService) service;

            mClingRegistryListener = new ClingRegistryListener(mUIHandler, upnpService.getControlPoint());

            upnpService.getRegistry().addListener(mClingRegistryListener);
            upnpService.getControlPoint().search();
        }

        public void onServiceDisconnected(ComponentName className) {
            upnpService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUIHandler = new LocalHandler(this);
        mCheckThread = new CheckThread(this, mUIHandler);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mFromMy = bundle.getBoolean(Constant.KEY_FROM_MY);
            mMiner = (Miner) bundle.getSerializable(Constant.KEY_MINER);
            if (mFromMy) {
                // Clear KEY_INSTALL_USB and check install again.
                PreferenceManager.getInstance().putBoolean(Constant.KEY_INSTALL_USB, false);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mCheckThread.doCheck();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindUpnpService();

        mCheckThread.quit();
        mCheckThread = null;
        mUIHandler.removeCallbacksAndMessages(null);
        mUIHandler = null;
    }

    @Override
    protected boolean onExitApp() {
        if (mFromMy) {
            return false;
        }
        return true;
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
        mTipText = findViewById(R.id.tv_progress_tip);
        mErrorText = findViewById(R.id.tv_reason);
        mTipButton = findViewById(R.id.btn_tip);
        mTipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mCheckCode) {
                    case Constant.CHECK_OS_FAILED:
                    case Constant.CHECK_DISK_FAILED:
                    case Constant.CHECK_TOUCH_FAILED:
                        finish();
                        getApplication().onTerminate();
                        break;

                    case Constant.CHECK_ADB_FAILED:
                    case Constant.CHECK_ADB_ALLOW_CHARGING_FAILED:
                    case Constant.CHECK_ADB_SAFE_FAILED:
                    case Constant.CHECK_INSTALL_FAILED:
                        Util.jumpToSettingADB(CheckDeviceActivity.this);
                        break;

                    case Constant.ACTION_CHECK_INSTALL:
                        mCheckThread.checkInstall();
                        break;

                    case Constant.CHECK_TOUCH_COPY_FAILED:
                        Touch.getInstance().connect(mUIHandler);
                        break;

                    case Constant.ACTION_CHECK_UPNP:
                        if (!NetworkHelper.getInstance().isWifiNetwork()) {
                            new AlertDialog.Builder(CheckDeviceActivity.this)
                                    .setMessage(getString(R.string.no_wifi))
                                    .setPositiveButton(R.string.ok, null)
                                    .create()
                                    .show();
                            return;
                        }
                        unbindUpnpService();
                        bindUpnpService();
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
                Touch.getInstance().connect(mUIHandler);
            }
        });
    }

    private void bindUpnpService() {
        org.seamless.util.logging.LoggingUtil.resetRootHandler(
                new FixedAndroidLogHandler()
        );
        bindService(
                new Intent(this, AndroidUpnpServiceImpl.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    private void unbindUpnpService() {
        if (upnpService != null) {
            upnpService.getRegistry().removeListener(mClingRegistryListener);
            upnpService.get().shutdown();
            // This will stop the UPnP service if nobody else is bound to it
            unbindService(serviceConnection);
        }
    }

    private void jumpToNextActivity() {
        if (mFromMy) {
            finish();
            return;
        }

        Intent intent = new Intent();
        if (!Wallet.exists()) {
            intent.putExtra(Constant.KEY_FROM_LAUNCHER, true);
            intent.setClass(this, WalletImporterActivity.class);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setClass(this, HomeActivity.class);
        }

        CustomApplication.sInstance.setPortArray(mDataPort, mHttpPort);
        DeviceInfo.get().setDataPort(mDataPort, mHttpPort);

        startActivity(intent);
        finish();
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

    private static class LocalHandler extends Handler {
        private WeakReference<CheckDeviceActivity> mContext;

        private LocalHandler(CheckDeviceActivity context) {
            this.mContext = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            final CheckDeviceActivity context = mContext.get();
            if (context == null) return;

            context.mProcessView.setVisibility(View.GONE);
            context.mResultView.setVisibility(View.VISIBLE);
            context.mProgressbar.setVisibility(View.GONE);
            context.mTipButton.setVisibility(View.VISIBLE);
            context.mImage.setVisibility(View.VISIBLE);
            context.mErrorText.setVisibility(View.VISIBLE);
            SpannableStringBuilder highlightText;
            context.mCheckCode = msg.what;
            switch (msg.what) {
                case Constant.CHECK_OS_FAILED:
                    context.mTitleText.setText(R.string.check_init);
                    context.mTipText.setText(R.string.check_device_error);
                    context.mTipButton.setText(R.string.check_btn_quit);
                    context.mImage.setImageResource(R.mipmap.check_failed);
                    context.mErrorText.setText(String.format("%s\n%s", context.getString(R.string.check_failed_reason), context.getString(R.string.check_fail_os)));
                    break;

                case Constant.CHECK_DISK_FAILED:
                    context.mTitleText.setText(R.string.check_init);
                    context.mTipText.setText(R.string.check_device_error);
                    context.mTipButton.setText(R.string.check_btn_quit);
                    context.mImage.setImageResource(R.mipmap.check_failed);
                    context.mErrorText.setText(String.format("%s\n%s", context.getString(R.string.check_failed_reason), context.getString(R.string.check_fail_disk)));
                    break;

                case Constant.CHECK_ADB_FAILED:
                    context.mCheckThread.setShouldPing(true);

                    context.mTitleText.setText(R.string.check_USB);
                    highlightText = context.createHighlight(context.getString(R.string.check_fail_usb),
                            context.getString(R.string.check_highlight_developer_options), context.getString(R.string.check_highlight_USB_debug));
                    context.mTipText.setText(highlightText);
                    context.mTipButton.setText(R.string.check_btn_adb);
                    context.mImage.setImageResource(R.mipmap.check_usb);
                    context.mErrorText.setVisibility(View.GONE);
                    break;

                case Constant.CHECK_ADB_ALLOW_CHARGING_FAILED:
                    context.mCheckThread.setShouldPing(true);

                    context.mTitleText.setText(R.string.check_USB);
                    context.mTipButton.setText(R.string.check_btn_adb);
                    highlightText = context.createHighlight(context.getString(R.string.check_fail_usb), context.getString(R.string.check_highlight_adb_install_need_confirm), context.getString(R.string.check_highlight_allow_charging_adb));

                    context.mImage.setImageResource(R.mipmap.check_usb_huawei);
                    context.mTipText.setText(highlightText);
                    context.mErrorText.setVisibility(View.GONE);
                    break;

                case Constant.CHECK_TCP_FAILED:
                    context.mTitleText.setText(R.string.check_tcp);
                    context.mTipText.setText(R.string.check_fail_tcp);
                    context.mTipButton.setVisibility(View.GONE);
                    context.mImage.setImageResource(R.mipmap.check_failed);
                    context.mErrorText.setText(R.string.download_tcp_tool);
                    break;

                case Constant.ACTION_CHECK_AUTH:
                    context.mTitleText.setText(R.string.check_authorization);
                    context.mProcessView.setVisibility(View.VISIBLE);
                    context.mResultView.setVisibility(View.GONE);
                    context.findViewById(R.id.pb_progress).setVisibility(View.GONE);

                    String RSAKey = (String) msg.obj;
                    highlightText = context.createHighlight(context.getString(R.string.check_authorization_tip), RSAKey,
                            context.getString(R.string.check_always_allow));
                    context.setProcessTip(highlightText, View.TEXT_ALIGNMENT_TEXT_START);
                    context.mResetButton.setVisibility(View.VISIBLE);
                    break;

                case Constant.CHECK_TOUCH_FAILED:
                    context.mTitleText.setText(R.string.check_touch);
                    context.mTipText.setText(R.string.check_fail_touch);
                    context.mTipButton.setText(R.string.check_btn_quit);
                    context.mImage.setImageResource(R.mipmap.check_failed);
                    context.mErrorText.setVisibility(View.GONE);
                    break;

                case Constant.CHECK_TOUCH_COPY_FAILED:
                    context.mTitleText.setText(R.string.check_touch);
                    context.mTipText.setText(R.string.check_failed);
                    context.mTipButton.setText(R.string.check_failed_action);
                    context.mImage.setImageResource(R.mipmap.check_failed);
                    context.mErrorText.setVisibility(View.GONE);
                    break;

                case Constant.CHECK_ADB_SAFE_FAILED:
                    context.mCheckThread.setShouldPing(true);

                    context.mTitleText.setText(R.string.check_touch_safe);
                    highlightText = context.createHighlight(context.getString(R.string.check_fail_adb_safe),
                            context.getString(R.string.check_highlight_safe));
                    context.mTipText.setText(highlightText);
                    context.mTipButton.setText(R.string.check_btn_adb);
                    context.mImage.setImageResource(R.mipmap.check_adb_safe);
                    context.mErrorText.setVisibility(View.GONE);
                    break;

                case Constant.ACTION_CHECK_INSTALL:
                    context.mProcessView.setVisibility(View.VISIBLE);
                    context.mResultView.setVisibility(View.GONE);
                    context.mTitleText.setText(R.string.check_installation);
                    context.findViewById(R.id.pb_progress).setVisibility(View.VISIBLE);
                    highlightText = context.createHighlight(context.getString(R.string.check_installation_tip),
                            context.getString(R.string.check_highlight_installation));
                    context.setProcessTip(highlightText, View.TEXT_ALIGNMENT_CENTER);
                    context.mResetButton.setVisibility(View.GONE);

                    context.mTipButton.setText(R.string.check_again);
                    context.mTipButton.setVisibility(View.VISIBLE);

                    if (context.mCheckThread != null) {
                        context.mCheckThread.checkInstall();
                    }
                    break;

                case Constant.CHECK_INSTALL_FAILED:
                    context.mCheckThread.setShouldPing(true);

                    context.mTitleText.setText(R.string.check_installation);
                    highlightText = context.createHighlight(context.getString(R.string.check_fail_installation),
                            context.getString(R.string.check_highlight_install));
                    context.mTipText.setText(highlightText);
                    context.mTipButton.setText(R.string.check_btn_adb);
                    context.mImage.setImageResource(R.mipmap.check_usb_installation);
                    context.mErrorText.setVisibility(View.GONE);
                    break;

                case Constant.ACTION_CHECK_UPNP:
                    if (!NetworkHelper.getInstance().isWifiNetwork()) {
                        new AlertDialog.Builder(context)
                                .setMessage(context.getString(R.string.no_wifi))
                                .setPositiveButton(R.string.ok, null)
                                .create()
                                .show();

                        context.mTitleText.setText(R.string.check_network);
                        context.mTipText.setText(R.string.check_network_error);
                        context.mTipButton.setText(R.string.check_again);
                        context.mImage.setImageResource(R.mipmap.check_failed);
                        context.mErrorText.setVisibility(View.GONE);
                    } else {
                        context.mProgressbar.setVisibility(View.VISIBLE);
                        context.mTitleText.setText(R.string.check_init);
                        context.mTipText.setText(R.string.check_init);
                        context.mTipButton.setVisibility(View.GONE);
                        context.mImage.setVisibility(View.GONE);
                        context.mErrorText.setVisibility(View.GONE);

                        context.bindUpnpService();
                    }

                    context.mCheckThread.turnOnStay();
                    context.mCheckThread.adbInstallConfirmOff();

                    // Timeout for upnp.
                    if (context.mUIHandler != null) {
                        context.mUIHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (context.mUIHandler != null) {
                                    context.jumpToNextActivity();
                                }
                            }
                        }, 15 * 1000);
                    }
                    break;

                case Constant.CHECK_UPNP_COMPLETE:
                    context.mDataPort = msg.arg1;
                    context.mHttpPort = msg.arg2;
                    // We ignore UPNP result here for proxy solution.
                    context.mTitleText.setText(R.string.check_success);
                    context.mImage.setImageResource(R.mipmap.check_success);
                    context.mTipButton.setVisibility(View.GONE);
                    context.mErrorText.setVisibility(View.GONE);
                    context.mTipText.setText(R.string.check_success_tip);

                    context.mImage.setVisibility(View.VISIBLE);
                    context.mTipText.setVisibility(View.VISIBLE);

                    if (context.mUIHandler != null) {
                        context.mUIHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                context.jumpToNextActivity();
                            }
                        }, 500);
                    }
                    /*if (context.mDataPort > 0) {
                        // UPNP success.
                        context.mTitleText.setText(R.string.check_success);
                        context.mImage.setImageResource(R.mipmap.check_success);
                        context.mTipButton.setVisibility(View.GONE);
                        context.mErrorText.setVisibility(View.GONE);
                        context.mTipText.setText(R.string.check_success_tip);

                        context.mImage.setVisibility(View.VISIBLE);
                        context.mTipText.setVisibility(View.VISIBLE);

                        if (context.mUIHandler != null) {
                            context.mUIHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    context.jumpToNextActivity();
                                }
                            }, 500);
                        }
                    } else {
                        context.mTitleText.setText(R.string.check_network);
                        context.mTipText.setText(R.string.check_network_error);
                        context.mTipButton.setText(R.string.check_again);
                        context.mImage.setImageResource(R.mipmap.check_failed);
                        context.mErrorText.setVisibility(View.GONE);

                        context.mCheckCode = Constant.ACTION_CHECK_UPNP;
                    }*/
                    break;

                default:
                    break;
            }
        }
    }
}
