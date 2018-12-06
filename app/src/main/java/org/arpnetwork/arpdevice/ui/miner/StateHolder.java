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

package org.arpnetwork.arpdevice.ui.miner;

import android.content.Context;
import android.content.Intent;

import org.arpnetwork.arpdevice.database.TransactionRecord;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.arpnetwork.arpdevice.constant.Constant.KEY_ADDRESS;
import static org.arpnetwork.arpdevice.constant.Constant.KEY_OP;
import static org.arpnetwork.arpdevice.constant.Constant.KEY_TX_HASH;

public class StateHolder {
    public static final int STATE_APPROVE_RUNNING = 1;
    public static final int STATE_APPROVE_SUCCESS = 2;
    public static final int STATE_APPROVE_FAILED = 3;

    public static final int STATE_BIND_RUNNING = 4;
    public static final int STATE_BIND_SUCCESS = 5;
    public static final int STATE_BIND_FAILED = 6;

    public static final int STATE_UNBIND_RUNNING = 7;
    public static final int STATE_UNBIND_SUCCESS = 8;
    public static final int STATE_UNBIND_FAILED = 9;

    public static final int STATE_DEPOSIT_RUNNING = 10;
    public static final int STATE_DEPOSIT_SUCCESS = 11;
    public static final int STATE_DEPOSIT_FAILED = 12;

    public static final int STATE_BANK_APPROVE_RUNNING = 13;
    public static final int STATE_BANK_APPROVE_SUCCESS = 14;
    public static final int STATE_BANK_APPROVE_FAILED = 15;

    public static final int STATE_BANK_CASH_RUNNING = 16;
    public static final int STATE_BANK_CASH_SUCCESS = 17;
    public static final int STATE_BANK_CASH_FAILED = 18;

    public static final int STATE_BANK_WITHDRAW_RUNNING = 19;
    public static final int STATE_BANK_WITHDRAW_SUCCESS = 20;
    public static final int STATE_BANK_WITHDRAW_FAILED = 21;

    private static ConcurrentHashMap<TaskInfo, Integer> sTaskStateMap = new ConcurrentHashMap<>(1);

    public static void setState(TaskInfo task, int status) {
        TaskInfo oldTask = getTaskByState(status);
        if (oldTask != null) sTaskStateMap.remove(oldTask);
        sTaskStateMap.put(task, status);
    }

    public static Integer getState(TaskInfo task) {
        return sTaskStateMap.get(task);
    }

    public static TaskInfo getTaskByState(int state) {
        TaskInfo key = null;

        for (TaskInfo getKey : sTaskStateMap.keySet()) {
            if (sTaskStateMap.get(getKey).equals(state)) {
                key = getKey;
            }
        }
        return key;
    }

    public static void clearAllState() {
        sTaskStateMap.clear();
    }

    public static void syncLocalState(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<TransactionRecord> transactionRecords = TransactionRecord.findAll();
                for (final TransactionRecord record : transactionRecords) {
                    String txHash = record.hash;
                    startService(context, txHash, record.opType, record.args);
                }
            }
        }).start();
    }

    private static void startService(final Context context, String transactionHash, int opType, String args) {
        Intent serviceIntent = new Intent(context, BindMinerIntentService.class);
        serviceIntent.putExtra(KEY_OP, opType);
        serviceIntent.putExtra(KEY_ADDRESS, args);
        serviceIntent.putExtra(KEY_TX_HASH, transactionHash);
        context.startService(serviceIntent);
    }
}
