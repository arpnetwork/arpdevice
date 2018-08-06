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

import org.arpnetwork.arpdevice.contracts.tasks.ARPBalanceTask;
import org.arpnetwork.arpdevice.contracts.tasks.ETHBalanceTask;
import org.arpnetwork.arpdevice.contracts.tasks.OnValueResult;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.http.HttpService;

import java.math.BigDecimal;

public class BalanceAPI {
    private static String ETHER_NODE = "http://dev.arpnetwork.org:8545";

    public static Web3j getWeb3J() {
        return Web3jFactory.build(new HttpService(ETHER_NODE));
    }

    public static void getEtherBalance(String address, OnValueResult<BigDecimal> onResult) {
        ETHBalanceTask ethBalanceTask = new ETHBalanceTask(onResult);
        ethBalanceTask.execute(address);
    }

    public static void getArpBalance(String address, OnValueResult<BigDecimal> onResult) {
        ARPBalanceTask arpBalanceTask = new ARPBalanceTask(onResult);
        arpBalanceTask.execute(address);
    }
}
