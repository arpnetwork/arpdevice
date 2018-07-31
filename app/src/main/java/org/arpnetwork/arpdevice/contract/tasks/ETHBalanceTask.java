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

package org.arpnetwork.arpdevice.contract.tasks;

import android.os.AsyncTask;

import org.arpnetwork.arpdevice.contract.BalanceAPI;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;

public class ETHBalanceTask extends AsyncTask<String, String, BigDecimal> {
    private OnValueResult onResult;

    public ETHBalanceTask (OnValueResult onValueResult) {
        onResult = onValueResult;
    }

    @Override
    protected BigDecimal doInBackground(String... param) {
        String address = param[0];
        EthGetBalance balance = null;
        try {
            balance = BalanceAPI.getWeb3J().ethGetBalance(
                    address, DefaultBlockParameterName.LATEST).send();
        } catch (IOException ignored) {
        } catch (RuntimeException ignored) {
        }

        if (balance == null) {
            return null;
        }

        return Convert.fromWei(balance.getBalance().toString(), Convert.Unit.ETHER);
    }

    @Override
    protected void onPostExecute(BigDecimal result) {
        if (!isCancelled() && onResult != null) {
            onResult.onValueResult(result);
        }
    }
}
