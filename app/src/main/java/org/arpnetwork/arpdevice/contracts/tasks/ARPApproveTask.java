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

import org.arpnetwork.arpdevice.contracts.ARPContract;
import org.arpnetwork.arpdevice.contracts.api.BalanceAPI;
import org.arpnetwork.arpdevice.contracts.api.VerifyAPI;
import org.arpnetwork.arpdevice.ui.wallet.WalletManager;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigInteger;

public class ARPApproveTask extends AsyncTask<String, String, Boolean> {
    // TODO: change to contract address
    private static final String SPENDER_ADDRESS = "0xe1c62093f55e8d7a86198dd8186e6de414b3fae4";
    private OnValueResult<Boolean> onResult;

    public ARPApproveTask(OnValueResult<Boolean> onValueResult) {
        onResult = onValueResult;
    }

    @Override
    protected Boolean doInBackground(String... param) {
        String password = param[0];
        String gasPrice = param[1];
        try {
            String hexData = getTransactionHexData(SPENDER_ADDRESS,
                    WalletManager.getInstance().loadCredentials(password),
                    new BigInteger(gasPrice), new BigInteger("400000"));
            EthSendTransaction raw = BalanceAPI.getWeb3J().ethSendRawTransaction(hexData).send();
        } catch (IOException ignore) {
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (!isCancelled() && onResult != null) {
            onResult.onValueResult(result);
        }
    }

    public static String getTransactionHexData(String address, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        ARPContract contract = ARPContract.load(BalanceAPI.getWeb3J(), credentials, gasPrice, gasLimit);
        String hexData = VerifyAPI.getRawTransaction(gasPrice, gasLimit, ARPContract.CONTRACT_ADDRESS,
                contract.getApproveFunctionData(address, new BigInteger(Convert.toWei("1000", Convert.Unit.ETHER).toString())), credentials);
        return hexData;
    }
}
