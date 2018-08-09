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

package org.arpnetwork.arpdevice.contracts.tasks;

import android.os.AsyncTask;

import org.arpnetwork.arpdevice.contracts.api.TransactionAPI;
import org.web3j.protocol.core.methods.request.Transaction;

import java.io.IOException;
import java.math.BigInteger;

public class TransactionGasEstimateTask extends AsyncTask<String, String, BigInteger> {
    private Transaction mTransaction;
    private OnValueResult<BigInteger> mOnResultListener;

    public TransactionGasEstimateTask(Transaction transaction, OnValueResult<BigInteger> onValueResult) {
        mOnResultListener = onValueResult;
        mTransaction = transaction;
    }

    @Override
    protected BigInteger doInBackground(String... param) {
        BigInteger gas = BigInteger.ZERO;
        try {
            gas = TransactionAPI.getTransactionGasLimit(mTransaction);
        } catch (IOException ignore) {
        }

        return gas;
    }

    @Override
    protected void onPostExecute(BigInteger result) {
        if (!isCancelled() && mOnResultListener != null) {
            mOnResultListener.onValueResult(result);
        }
    }
}
