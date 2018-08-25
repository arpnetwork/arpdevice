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

import org.arpnetwork.arpdevice.ui.wallet.Wallet;
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
import java.util.concurrent.ExecutionException;

public class TransactionAPI {
    private static final String DEFAULT_GAS_LIMIT = "400000";

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
        return gas.hasError() ? new BigInteger(DEFAULT_GAS_LIMIT) : gas.getAmountUsed();
    }

    public static BigInteger getAsyncTransactionGasLimit(Transaction transaction) {
        EthEstimateGas gas = null;
        try {
            gas = EtherAPI.getWeb3J().ethEstimateGas(transaction).sendAsync().get();
        } catch (InterruptedException ignore) {
        } catch (ExecutionException ignore) {
        }
        return (gas == null || gas.hasError()) ? new BigInteger(DEFAULT_GAS_LIMIT) : gas.getAmountUsed();
    }

    public static BigInteger estimateFunctionGasLimit(String functionString, String contractAddress) {
        String ownerAddress = Wallet.get().getAddress();
        Transaction transaction = Transaction.createEthCallTransaction(ownerAddress, contractAddress, functionString);
        return TransactionAPI.getAsyncTransactionGasLimit(transaction);
    }

    public static boolean isStatusOK(String status) {
        if (null == status) {
            return true;
        }
        BigInteger statusQuantity = Numeric.decodeQuantity(status);
        return BigInteger.ONE.equals(statusQuantity);
    }

    private static BigInteger getNonce(String address) {
        EthGetTransactionCount transactionCount = new EthGetTransactionCount();
        try {
            transactionCount = EtherAPI.getWeb3J()
                    .ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).send();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return transactionCount.getTransactionCount();
    }

    private static BigInteger getNonceAsync(String address) throws ExecutionException, InterruptedException {
        EthGetTransactionCount transactionCount = EtherAPI.getWeb3J()
                .ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).sendAsync().get();
        return transactionCount.getTransactionCount();
    }

    private static RawTransaction getTransaction(BigInteger gasPrice, BigInteger gasLimit,
            String contractAddress, String data, Credentials credentials) {
        BigInteger nonce = null;
        try {
            nonce = getNonceAsync(credentials.getAddress());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce, gasPrice, gasLimit, contractAddress, data);
        return rawTransaction;
    }
}
