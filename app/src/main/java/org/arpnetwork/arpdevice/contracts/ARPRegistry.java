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

package org.arpnetwork.arpdevice.contracts;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.contracts.api.TransactionAPI;
import org.arpnetwork.arpdevice.ui.bean.BindPromise;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tuples.generated.Tuple5;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;

public class ARPRegistry extends Contract {
    public static final String CONTRACT_ADDRESS = "0x9f6b469dd5ec3e86f19cac817a2bc802ae54520d";
    private static final String BINARY = null;

    public static final String FUNC_SERVERS = "servers";

    public static final String FUNC_BINDINGS = "bindings";

    public static final String FUNC_BINDDEVICE = "bindDevice";

    public static final String FUNC_UNBINDDEVICE = "unbindDevice";

    public static final String FUNC_SERVERBYINDEX = "serverByIndex";

    public static final String FUNC_SERVERCOUNT = "serverCount";

    protected ARPRegistry(String contractAddress, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, EtherAPI.getWeb3J(), credentials, gasPrice, gasLimit);
    }

    protected ARPRegistry(String contractAddress, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, EtherAPI.getWeb3J(), transactionManager, gasPrice, gasLimit);
    }

    public static Function funcBindDevice(String _server, BigInteger _amount, BigInteger _expired, BigInteger _signExpired, BigInteger _v, byte[] _r, byte[] _s) {
        Function function = new Function(
                FUNC_BINDDEVICE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_server),
                        new org.web3j.abi.datatypes.generated.Uint256(_amount),
                        new org.web3j.abi.datatypes.generated.Uint256(_expired),
                        new org.web3j.abi.datatypes.generated.Uint256(_signExpired),
                        new org.web3j.abi.datatypes.generated.Uint8(_v),
                        new org.web3j.abi.datatypes.generated.Bytes32(_r),
                        new org.web3j.abi.datatypes.generated.Bytes32(_s)),
                Collections.<TypeReference<?>>emptyList());
        return function;
    }

    public static Function funcUnbindDevice() {
        Function function = new Function(
                FUNC_UNBINDDEVICE,
                Arrays.<Type>asList(),
                Collections.<TypeReference<?>>emptyList());
        return function;
    }

    public RemoteCall<TransactionReceipt> bindDevice(String _server, BigInteger _amount, BigInteger _expired, BigInteger _signExpired, BigInteger _v, byte[] _r, byte[] _s) {
        final Function function = funcBindDevice(_server, _amount, _expired, _signExpired, _v, _r, _s);
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> unbindDevice() {
        final Function function = funcUnbindDevice();
        return executeRemoteCallTransaction(function);
    }

    public static ARPRegistry load(String contractAddress, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ARPRegistry(contractAddress, credentials, gasPrice, gasLimit);
    }

    public static ARPRegistry load(String contractAddress, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ARPRegistry(contractAddress, transactionManager, gasPrice, gasLimit);
    }

    // estimate gas limit

    public static BigInteger estimateBindDeviceGasLimit(String _server, BindPromise bindPromise) {
        String functionString = FunctionEncoder.encode(funcBindDevice(_server, bindPromise.getAmount(),
                bindPromise.getExpired(), bindPromise.getSignExpired(),
                new BigInteger(String.valueOf(bindPromise.getSignatureData().getV())),
                bindPromise.getSignatureData().getR(), bindPromise.getSignatureData().getS()));
        return TransactionAPI.estimateFunctionGasLimit(functionString, CONTRACT_ADDRESS);
    }

    public static BigInteger estimateUnbindGasLimit() {
        String functionString = FunctionEncoder.encode(funcUnbindDevice());
        return TransactionAPI.estimateFunctionGasLimit(functionString, CONTRACT_ADDRESS);
    }

    // Bind miner helper method.
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

        List<Type> results = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        if (results.size() == 0) {
            return null;
        }
        return (BigInteger) results.get(0).getValue();
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
        if (results.size() == 0) {
            return null;
        }
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

        List<Type> results = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        if (results.size() == 0) {
            return null;
        }
        return new Tuple2<String, BigInteger>(
                (String) results.get(0).getValue(),
                (BigInteger) results.get(1).getValue());
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

        List<Type> results = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());

        if (results.size() == 0) {
            return null;
        }
        return new Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>(
                (BigInteger) results.get(0).getValue(),
                (BigInteger) results.get(1).getValue(),
                (BigInteger) results.get(2).getValue(),
                (BigInteger) results.get(3).getValue());
    }
}
