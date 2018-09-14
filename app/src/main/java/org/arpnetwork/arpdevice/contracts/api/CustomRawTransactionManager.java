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

package org.arpnetwork.arpdevice.contracts.api;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.response.TransactionReceiptProcessor;

import java.io.IOException;
import java.math.BigInteger;

public class CustomRawTransactionManager extends RawTransactionManager {
    public interface OnHashBackListener {
        void onHash(String transactionHash);
    }

    private OnHashBackListener mListener;

    public void setListener(OnHashBackListener listener) {
        mListener = listener;
    }

    public CustomRawTransactionManager(Web3j web3j, Credentials credentials, byte chainId) {
        super(web3j, credentials, chainId);
    }

    public CustomRawTransactionManager(Web3j web3j, Credentials credentials, byte chainId, TransactionReceiptProcessor transactionReceiptProcessor) {
        super(web3j, credentials, chainId, transactionReceiptProcessor);
    }

    public CustomRawTransactionManager(Web3j web3j, Credentials credentials, byte chainId, int attempts, long sleepDuration) {
        super(web3j, credentials, chainId, attempts, sleepDuration);
    }

    public CustomRawTransactionManager(Web3j web3j, Credentials credentials) {
        super(web3j, credentials);
    }

    public CustomRawTransactionManager(Web3j web3j, Credentials credentials, int attempts, int sleepDuration) {
        super(web3j, credentials, attempts, sleepDuration);
    }

    @Override
    public EthSendTransaction sendTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value) throws IOException {
        EthSendTransaction ethSendTransaction = super.sendTransaction(gasPrice, gasLimit, to, data, value);
        if (mListener != null) {
            mListener.onHash(ethSendTransaction.getTransactionHash());
        }
        return ethSendTransaction;
    }
}
