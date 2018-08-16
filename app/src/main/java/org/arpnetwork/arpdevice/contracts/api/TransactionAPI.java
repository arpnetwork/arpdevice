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
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;

public class TransactionAPI {

    /**
     * Get row transaction string by params
     *
     * @param gasPrice
     * @param gasLimit
     * @param contractAddress
     * @param data
     * @param credentials
     * @return
     */
    public static String getRawTransaction(BigInteger gasPrice, BigInteger gasLimit,
            String contractAddress, String data, Credentials credentials) {
        RawTransaction transaction = getTransaction(gasPrice, gasLimit, contractAddress, data, credentials);
        byte[] signedMessage = TransactionEncoder.signMessage(transaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        return hexValue;
    }

    public static BigInteger getTransactionGasLimit(Transaction transaction) throws IOException {
        EthEstimateGas gas = EtherAPI.getWeb3J().ethEstimateGas(transaction).send();
        return gas.getAmountUsed();
    }

    private static BigInteger getTransactionCount(String address) {
        EthGetTransactionCount transactionCount = new EthGetTransactionCount();
        try {
            transactionCount = EtherAPI.getWeb3J()
                    .ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).send();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return transactionCount.getTransactionCount();
    }

    private static RawTransaction getTransaction(BigInteger gasPrice, BigInteger gasLimit,
            String contractAddress, String data, Credentials credentials) {
        BigInteger nonce = getTransactionCount(credentials.getAddress());
        RawTransaction rawTransaction  = RawTransaction.createTransaction(
                nonce, gasPrice, gasLimit, contractAddress, data);
        return rawTransaction;
    }
}
