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

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.config.Constant;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.data.BankAllowance;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.miner.BindMinerHelper;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;

import java.math.BigInteger;
import java.util.Timer;
import java.util.TimerTask;

public class MonitorService extends Service {
    private Handler mHandler;
    private Timer mTimer;

    public static void startDialogActivity(String args) {
        ExchangeDialogActivity.launch(CustomApplication.sInstance, args);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new MyHandler();
        mTimer = new Timer();

        mTimer.schedule(mTimerTask, Config.MONITOR_MINER_INTERVAL, Config.MONITOR_MINER_INTERVAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        runTask();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTimer.cancel();
    }

    private boolean getUnexchange(BankAllowance allowance, Miner miner, Promise promise) {
        BigInteger amount = new BigInteger(promise.getAmount(), 16);
        BigInteger unexchanged = amount.subtract(allowance.paid);
        if (unexchanged.compareTo(BigInteger.ZERO) > 0) {
            Message message = new Message();
            message.what = 1;
            message.obj = miner.getExpired().longValue() + "#" + unexchanged;
            mHandler.sendMessage(message);
            return true;
        }
        return false;
    }

    private void runTask() {
        String walletAddr = Wallet.get().getAddress();
        Miner miner = BindMinerHelper.getBound(walletAddr);
        if (miner != null) {
            BankAllowance allowance = ARPBank.allowance(miner.getAddress(), walletAddr);
            if (allowance != null) {
                allowance.save();

                Promise promise = Promise.get();
                if (promise != null && new BigInteger(promise.getCid(), 16).compareTo(allowance.id) != 0) {
                    Promise.clear();
                    promise = null;
                }

                boolean exchange = false;
                if (miner.getExpired().compareTo(BigInteger.ZERO) > 0
                        && promise != null
                        && !TextUtils.isEmpty(promise.getAmount())) {
                    exchange = getUnexchange(allowance, miner, promise);
                }

                if (!exchange && (!allowance.valid() || !miner.expiredValid())) {
                    Intent intent = new Intent();
                    intent.setAction(Constant.BROADCAST_ACTION_STATE_CHANGED);
                    LocalBroadcastManager.getInstance(CustomApplication.sInstance).sendBroadcast(intent);
                }
            }
        }
    }

    private TimerTask mTimerTask = new TimerTask() {
        @Override
        public void run() {
            runTask();
        }
    };

    private static class MyHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            startDialogActivity((String) message.obj);
        }
    }
}
