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
import org.web3j.tuples.generated.Tuple5;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class BindMinerHelper {

    public static List<Miner> getMinerList() {
        List<Miner> list = new ArrayList<Miner>();
        try {
            BigInteger serverCount = serverCount();
            for (int i = 0; i < serverCount.intValue(); i++) {
                Tuple5<String, BigInteger, BigInteger, BigInteger, BigInteger> server = serverByIndex(BigInteger.valueOf(i));
                Miner miner = new Miner();
                miner.address = server.getValue1();
                miner.ip = server.getValue2();
                miner.port = server.getValue3();
                miner.size = server.getValue4();
                miner.expired = server.getValue5();
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

    public static Miner getBound(String address) {
        Miner miner = null;
        try {
            byte[] address32 = Util.stringToBytes32(address);

            Tuple2<String, BigInteger> binder = bindings(address32);
            if (!TextUtils.isEmpty(binder.getValue1()) && !binder.getValue1().
                    equals("0x0000000000000000000000000000000000000000")) {
                miner = new Miner();
                miner.address = binder.getValue1();
                miner.expired = binder.getValue2();
            }
        } catch (ExecutionException e) {
        } catch (InterruptedException e) {
        }
        return miner;
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
}
