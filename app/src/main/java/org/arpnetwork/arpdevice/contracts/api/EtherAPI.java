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

import android.content.Context;

import org.arpnetwork.arpdevice.CustomApplication;
import org.arpnetwork.arpdevice.contracts.tasks.ETHBalanceTask;
import org.arpnetwork.arpdevice.contracts.tasks.OnValueResult;
import org.arpnetwork.arpdevice.util.Util;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;

public class EtherAPI {
    private static String ETHER_NODE = "http://dev.arpnetwork.org:8545";

    private static Web3j sWeb3j;

    public static synchronized Web3j getWeb3J() {
        if (sWeb3j == null) {
            HttpService httpService = new HttpService(ETHER_NODE);
            Context context = CustomApplication.sInstance;
            String userAgent = Util.getAppName(context) + "/" + Util.getAppVersionCode(context);
            httpService.addHeader("User-Agent", userAgent);
            sWeb3j = Web3jFactory.build(httpService);
        }
        return sWeb3j;
    }

    public static void getEtherBalance(String address, OnValueResult<BigInteger> onResult) {
        ETHBalanceTask ethBalanceTask = new ETHBalanceTask(onResult);
        ethBalanceTask.execute(address);
    }

    public static long getTransferDate(String blockHash) throws ExecutionException, InterruptedException {
        EthBlock.Block ethBlock = getWeb3J().ethGetBlockByHash(blockHash, false).sendAsync().get().getBlock();
        return ethBlock.getTimestamp().longValue() * 1000;
    }
}
