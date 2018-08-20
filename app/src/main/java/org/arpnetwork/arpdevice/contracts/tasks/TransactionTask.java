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

import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.util.TransactionUtil;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.IOException;

public class TransactionTask extends AsyncTask<String, String, Boolean> {
    private OnValueResult<Boolean> onResult;

    public TransactionTask(OnValueResult<Boolean> onValueResult) {
        onResult = onValueResult;
    }

    @Override
    protected Boolean doInBackground(String... param) {
        Boolean result;
        String txHexData = param[0];
        try {
            EthSendTransaction transaction = EtherAPI.getWeb3J().ethSendRawTransaction(txHexData).send();
            TransactionReceipt transactionReceipt = TransactionUtil.waitForTransactionReceipt(transaction.getTransactionHash());
            result = transactionReceipt.getStatus().equals("0x1");
        } catch (IOException e) {
            result = null;
        }
        return result;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (!isCancelled() && onResult != null) {
            onResult.onValueResult(result);
        }
    }
}
