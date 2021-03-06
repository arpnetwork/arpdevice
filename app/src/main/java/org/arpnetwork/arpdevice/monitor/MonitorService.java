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

package org.arpnetwork.arpdevice.monitor;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.constant.Constant;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.data.BankAllowance;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.miner.BindMinerHelper;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Timer;
import java.util.TimerTask;

public class MonitorService extends Service {
    private static final int id = 0x1fff;
    public static final int MSG_EXCHANGE = 1;
    private Handler mHandler;
    private Timer mTimer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new MyHandler();
        startForeground(id, getNotification(getString(R.string.notify_running)));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTimer();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopTimer();

        super.onDestroy();
    }

    private void startTimer() {
        stopTimer();

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                String walletAddr = Wallet.get().getAddress();
                Miner miner = null;
                try {
                    miner = BindMinerHelper.getBound(walletAddr);
                } catch (IOException ignored) {
                }
                if (miner != null) {
                    BankAllowance allowance = null;
                    try {
                        allowance = ARPBank.allowance(miner.getAddress(), walletAddr);
                    } catch (IOException ignored) {
                    }
                    if (allowance != null) {
                        allowance.save();

                        Promise promise = Promise.get();
                        if (promise != null && promise.getCidBig().compareTo(allowance.id) != 0) {
                            Promise.clear();
                            promise = null;
                        }

                        boolean exchange = false;
                        if (miner.getExpired().compareTo(BigInteger.ZERO) > 0
                                && promise != null
                                && !TextUtils.isEmpty(promise.getAmountRaw())) {
                            exchange = getUnexchange(allowance, miner, promise);
                        }

                        if (!exchange && (!allowance.valid() || !miner.expiredValid())) {
                            Intent intent = new Intent();
                            intent.setAction(Constant.BROADCAST_ACTION_STATE_CHANGED);
                            LocalBroadcastManager.getInstance(MonitorService.this).sendBroadcast(intent);
                        }
                    }
                }
            }
        }, 0, Config.MONITOR_MINER_INTERVAL);
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private boolean getUnexchange(BankAllowance allowance, Miner miner, Promise promise) {
        BigInteger amount = promise.getAmountBig();
        BigInteger unexchanged = amount.subtract(allowance.paid);
        if (unexchanged.compareTo(BigInteger.ZERO) > 0) {
            Message message = new Message();
            message.what = MSG_EXCHANGE;
            message.obj = miner.getExpired().longValue() + "#" + unexchanged;
            mHandler.sendMessage(message);
            return true;
        }
        return false;
    }

    private Notification getNotification(String text) {
        Notification notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        return notification;
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_EXCHANGE:
                    startDialogActivity((String) message.obj);
                    break;

                default:
                    break;
            }
        }
    }

    private void startDialogActivity(String args) {
        ExchangeDialogActivity.launch(MonitorService.this, args);
    }
}
