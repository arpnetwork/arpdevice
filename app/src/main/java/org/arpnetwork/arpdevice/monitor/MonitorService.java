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
import android.text.TextUtils;

import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.config.Config;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.data.BankAllowance;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.stream.Touch;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.miner.BindMinerHelper;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;

import java.math.BigInteger;
import java.util.Timer;
import java.util.TimerTask;

public class MonitorService extends Service {
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
        mTimer = new Timer();

        mTimer.schedule(mTimerTask, 0, Config.MONITOR_MINER_INTERVAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private TimerTask mTimerTask = new TimerTask() {
        @Override
        public void run() {
            if (Touch.getInstance().isRecording()) return;

            Miner miner = BindMinerHelper.getBound(Wallet.get().getAddress());
            if (miner == null || Promise.get() == null)
                return;

            if (miner.getExpired().compareTo(BigInteger.ZERO) > 0) {
                String amount = Promise.get().getAmount();
                if (!TextUtils.isEmpty(amount)) {
                    getUnexchange(miner);
                }
            }
        }
    };

    public static void startDialogActivity(String args) {
        ExchangeDialogActivity.launch(CustomApplication.sInstance, args);
    }

    private void getUnexchange(final Miner miner) {
        if (Promise.get() == null) return;

        final BigInteger amount = new BigInteger(Promise.get().getAmount(), 16);

        String owner = miner.getAddress();
        String spender = Wallet.get().getAddress();
        BankAllowance allowance = ARPBank.allowance(owner, spender);
        BigInteger unexchanged = amount.subtract(allowance.paid);
        if (unexchanged.compareTo(BigInteger.ZERO) > 0) {
            Message message = new Message();
            message.what = 1;
            message.obj = miner.getExpired().longValue() + "#" + unexchanged;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTimer.cancel();
    }

    private static class MyHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            startDialogActivity((String) message.obj);
        }
    }
}
