package org.arpnetwork.arpdevice.contracts.tasks;

import android.text.TextUtils;

import org.arpnetwork.arpdevice.contracts.ARPRegistry;
import org.arpnetwork.arpdevice.contracts.api.BalanceAPI;
import org.arpnetwork.arpdevice.ui.bean.Miner;
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
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tuples.generated.Tuple7;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class BindMinerHelper {
    public static final String CONTRACT_ADDRESS = "0xa3159b314777e6529bca4ca2000c6e6456496589";

    public static List<Miner> getMinerList() {
        List<Miner> list = new ArrayList<Miner>();
        try {
            BigInteger serverCount = serverCount();
            for (int i = 0; i < serverCount.intValue(); i++) {
                Tuple7<String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger> server = serverByIndex(BigInteger.valueOf(i));
                Miner miner = new Miner();
                miner.address = server.getValue1();
                miner.ip = server.getValue2();
                miner.port = server.getValue3();
                miner.capacity = server.getValue4();
                miner.amount = server.getValue5();
                miner.expired = server.getValue6();
                miner.deviceCount = server.getValue7();
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
        EthCall response = BalanceAPI.getWeb3J().ethCall(
                Transaction.createEthCallTransaction(null, CONTRACT_ADDRESS, encodedFunction),
                DefaultBlockParameterName.LATEST)
                .sendAsync().get();

        List<Type> someTypes = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        return (BigInteger) someTypes.get(0).getValue();
    }

    public static Tuple7<String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger> serverByIndex(BigInteger _index) throws ExecutionException, InterruptedException {
        final Function function = new Function(ARPRegistry.FUNC_SERVERBYINDEX,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_index)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }, new TypeReference<Uint32>() {
                }, new TypeReference<Uint16>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<Uint256>() {
                }));
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = BalanceAPI.getWeb3J().ethCall(
                Transaction.createEthCallTransaction(null, CONTRACT_ADDRESS, encodedFunction),
                DefaultBlockParameterName.LATEST)
                .sendAsync().get();

        List<Type> someTypes = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        return new Tuple7<String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>(
                (String) someTypes.get(0).getValue(),
                (BigInteger) someTypes.get(1).getValue(),
                (BigInteger) someTypes.get(2).getValue(),
                (BigInteger) someTypes.get(3).getValue(),
                (BigInteger) someTypes.get(4).getValue(),
                (BigInteger) someTypes.get(5).getValue(),
                (BigInteger) someTypes.get(6).getValue());
    }

    public static Miner getBinded(String address) {
        Miner miner = null;
        try {
            Tuple3<String, BigInteger, BigInteger> binder = devices(address);
            if (!TextUtils.isEmpty(binder.getValue1()) && !binder.getValue1().
                    equals("0x0000000000000000000000000000000000000000")) {
                miner = new Miner();
                miner.address = binder.getValue1();
                miner.amount = binder.getValue2();
                miner.expired = binder.getValue3();
            }
        } catch (ExecutionException e) {
        } catch (InterruptedException e) {
        }
        return miner;
    }

    // Check my address has bind miner.
    public static Tuple3<String, BigInteger, BigInteger> devices(String address) throws ExecutionException, InterruptedException {
        final Function function = new Function(ARPRegistry.FUNC_DEVICES,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(address)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<Uint256>() {
                }));

        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = BalanceAPI.getWeb3J().ethCall(
                Transaction.createEthCallTransaction(null, CONTRACT_ADDRESS, encodedFunction),
                DefaultBlockParameterName.LATEST)
                .sendAsync().get();

        List<Type> someTypes = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        return new Tuple3<String, BigInteger, BigInteger>(
                (String) someTypes.get(0).getValue(),
                (BigInteger) someTypes.get(1).getValue(),
                (BigInteger) someTypes.get(2).getValue());
    }
}