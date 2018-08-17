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

package org.arpnetwork.arpdevice.util;

import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public class TransactionUtil {
    private static final int SLEEP_DURATION = 1500;
    private static final int ATTEMPTS = 40;

    public static TransactionReceipt waitForTransactionReceipt(String transactionHash) {
        // see org.web3j.protocol.scenarios.CreateRawTransactionIT
        TransactionReceipt receiptOptional = null;
        try {
            receiptOptional = getTransactionReceipt(transactionHash, SLEEP_DURATION, ATTEMPTS);
        } catch (Exception e) {
        }
        return receiptOptional;
    }

    public static TransactionReceipt getTransactionReceipt(String transactionHash, int sleepDuration, int attempts) throws Exception {
        TransactionReceipt receiptOptional = sendTransactionReceiptRequest(transactionHash);
        for (int i = 0; i < attempts; i++) {
            if (receiptOptional == null) {
                Thread.sleep(sleepDuration);
                receiptOptional = sendTransactionReceiptRequest(transactionHash);
            } else {
                break;
            }
        }

        return receiptOptional;
    }

    public static TransactionReceipt sendTransactionReceiptRequest(
            String transactionHash) throws Exception {
        EthGetTransactionReceipt transactionReceipt = EtherAPI.getWeb3J().ethGetTransactionReceipt(transactionHash).send();
        return transactionReceipt.getTransactionReceipt();
    }
}
