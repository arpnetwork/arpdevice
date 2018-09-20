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

package org.arpnetwork.arpdevice.ui.miner;

import android.os.AsyncTask;
import android.text.TextUtils;

import org.arpnetwork.arpdevice.contracts.ARPRegistry;
import org.arpnetwork.arpdevice.contracts.tasks.GetBoundTask;
import org.arpnetwork.arpdevice.contracts.tasks.OnValueResult;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.util.Util;

import org.web3j.tuples.generated.Tuple2;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tuples.generated.Tuple5;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class BindMinerHelper {
    public static void getBoundAsync(String address, OnValueResult<Miner> onResult) {
        GetBoundTask task = new GetBoundTask(onResult);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, address);
    }

    public static Miner getBound(String address) throws IOException {
        Miner miner = null;
        byte[] address32 = Util.stringToBytes32(address);
        Tuple2<String, BigInteger> binder = ARPRegistry.bindings(address32);

        if (binder == null) return null;

        String serverAddr = binder.getValue1();
        if (!TextUtils.isEmpty(serverAddr) && !serverAddr.
                equals("0x0000000000000000000000000000000000000000")) {
            Tuple4<BigInteger, BigInteger, BigInteger, BigInteger> server = ARPRegistry.servers(serverAddr);
            if (server != null) {
                miner = new Miner();
                miner.setAddress(serverAddr);
                miner.setIp(server.getValue1());
                miner.setPort(server.getValue2());
                miner.setSize(server.getValue3());
                miner.setExpired(server.getValue4());
            }
        }
        return miner;
    }

    public static List<Miner> getMinerList() throws IOException {
        List<Miner> list = new ArrayList<Miner>();
        BigInteger serverCount = ARPRegistry.serverCount();
        for (int i = 0; i < serverCount.intValue(); i++) {
            Tuple5<String, BigInteger, BigInteger, BigInteger, BigInteger> server = ARPRegistry.serverByIndex(BigInteger.valueOf(i));
            if (server == null) {
                continue;
            }

            if (!server.getValue5().equals(BigInteger.ZERO)) { // drop expired != 0
                continue;
            }
            Miner miner = new Miner();
            miner.setAddress(server.getValue1());
            miner.setIp(server.getValue2());
            miner.setPort(server.getValue3());
            miner.setSize(server.getValue4());
            miner.setExpired(server.getValue5());
            list.add(miner);
        }
        return list;
    }
}
