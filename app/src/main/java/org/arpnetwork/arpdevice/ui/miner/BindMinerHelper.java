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

import android.text.TextUtils;

import org.arpnetwork.arpdevice.contracts.ARPRegistry;
import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.util.Util;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tuples.generated.Tuple5;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class BindMinerHelper {

    public static Miner getBound(String address) {
        Miner miner = null;
        try {
            byte[] address32 = Util.stringToBytes32(address);

            Tuple2<String, BigInteger> binder = bindings(address32);
            String serverAddr = binder.getValue1();
            if (!TextUtils.isEmpty(serverAddr) && !serverAddr.
                    equals("0x0000000000000000000000000000000000000000")) {
                Tuple4<BigInteger, BigInteger, BigInteger, BigInteger> server = servers(serverAddr);

                miner = new Miner();
                miner.setAddress(serverAddr);
                miner.setIp(server.getValue1());
                miner.setPort(server.getValue2());
                miner.setSize(server.getValue3());
                miner.setExpired(server.getValue4());
            }
        } catch (ExecutionException e) {
        } catch (InterruptedException e) {
        }
        return miner;
    }

    public static List<Miner> getMinerList() {
        List<Miner> list = new ArrayList<Miner>();
        try {
            BigInteger serverCount = serverCount();
            for (int i = 0; i < serverCount.intValue(); i++) {
                Tuple5<String, BigInteger, BigInteger, BigInteger, BigInteger> server = serverByIndex(BigInteger.valueOf(i));

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
        } catch (ExecutionException e) {
        } catch (InterruptedException e) {
        }
        return Collections.emptyList();
    }

    public static BigInteger serverCount() throws ExecutionException, InterruptedException {
        Function function = new Function(ARPRegistry.FUNC_SERVERCOUNT,
                Collections.<Type>emptyList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = EtherAPI.getWeb3J().ethCall(
                Transaction.createEthCallTransaction(null, ARPRegistry.CONTRACT_ADDRESS, encodedFunction),
                DefaultBlockParameterName.LATEST)
                .sendAsync().get();

        List<Type> someTypes = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        return (BigInteger) someTypes.get(0).getValue();
    }

    public static Tuple5<String, BigInteger, BigInteger, BigInteger, BigInteger> serverByIndex(BigInteger _index) throws ExecutionException, InterruptedException {
        final Function function = new Function(ARPRegistry.FUNC_SERVERBYINDEX,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_index)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                                                }, new TypeReference<Uint32>() {
                                                },
                        new TypeReference<Uint16>() {
                        }, new TypeReference<Uint256>() {
                        },
                        new TypeReference<Uint256>() {
                        }));
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = EtherAPI.getWeb3J().ethCall(
                Transaction.createEthCallTransaction(null, ARPRegistry.CONTRACT_ADDRESS, encodedFunction),
                DefaultBlockParameterName.LATEST)
                .sendAsync().get();

        List<Type> results = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        return new Tuple5<String, BigInteger, BigInteger, BigInteger, BigInteger>(
                (String) results.get(0).getValue(),
                (BigInteger) results.get(1).getValue(),
                (BigInteger) results.get(2).getValue(),
                (BigInteger) results.get(3).getValue(),
                (BigInteger) results.get(4).getValue());
    }

    // Check my address has bind miner.
    public static Tuple2<String, BigInteger> bindings(byte[] address32) throws ExecutionException, InterruptedException {
        Function function = new Function(ARPRegistry.FUNC_BINDINGS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(address32)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }, new TypeReference<Uint256>() {
                }));

        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = EtherAPI.getWeb3J().ethCall(
                Transaction.createEthCallTransaction(null, ARPRegistry.CONTRACT_ADDRESS, encodedFunction),
                DefaultBlockParameterName.LATEST)
                .sendAsync().get();

        List<Type> someTypes = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        return new Tuple2<String, BigInteger>(
                (String) someTypes.get(0).getValue(),
                (BigInteger) someTypes.get(1).getValue());
    }

    public static Tuple4<BigInteger, BigInteger, BigInteger, BigInteger> servers(String address) throws ExecutionException, InterruptedException {
        Function function = new Function(ARPRegistry.FUNC_SERVERS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(address)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>() {
                }, new TypeReference<Uint16>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<Uint256>() {
                }));

        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = EtherAPI.getWeb3J().ethCall(
                Transaction.createEthCallTransaction(null, ARPRegistry.CONTRACT_ADDRESS, encodedFunction),
                DefaultBlockParameterName.LATEST)
                .sendAsync().get();

        List<Type> someTypes = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        return new Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>(
                (BigInteger) someTypes.get(0).getValue(),
                (BigInteger) someTypes.get(1).getValue(),
                (BigInteger) someTypes.get(2).getValue(),
                (BigInteger) someTypes.get(3).getValue());
    }
}
