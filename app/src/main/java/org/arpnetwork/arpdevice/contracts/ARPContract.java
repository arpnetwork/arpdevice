package org.arpnetwork.arpdevice.contracts;

import android.util.Log;

import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.contracts.api.TransactionAPI;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.utils.Convert;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class ARPContract extends Contract {
    private static final String TAG = ARPContract.class.getSimpleName();
    public static final String CONTRACT_ADDRESS = "0xBeB6fdF4ef6CEb975157be43cBE0047B248a8922";
    public static final String APPROVE_ARP_NUMBER = "5000";

    public static final String FUNC_BALANCE_OF = "balanceOf";
    public static final String FUNC_ALLOWANCE = "allowance";
    public static final String FUNC_APPROVE = "approve";

    private ARPContract(Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super("", CONTRACT_ADDRESS, EtherAPI.getWeb3J(), credentials, gasPrice, gasLimit);
    }

    public static ARPContract load(Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ARPContract(credentials, gasPrice, gasLimit);
    }

    public static BigInteger allowance(String owner, String spender) {
        Function function = getAllowanceFunction(owner, spender);
        EthCall response = null;
        try {
            response = EtherAPI.getWeb3J().ethCall(
                    Transaction.createEthCallTransaction(owner, CONTRACT_ADDRESS, FunctionEncoder.encode(function)),
                    DefaultBlockParameterName.LATEST)
                    .sendAsync().get();
        } catch (Exception e) {
            Log.e(TAG, "allowance(" + owner + ", " + spender + "), error:" + e.getCause());
            return BigInteger.ZERO;
        }

        List<Type> someTypes = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        if (someTypes.size() == 0) return BigInteger.ZERO;
        Uint balance = (Uint) someTypes.get(0);
        return balance.getValue();
    }

    public static BigInteger balanceOf(String owner) {
        Function function = getBalanceOfFunction(owner);
        EthCall response = null;
        try {
            response = EtherAPI.getWeb3J().ethCall(
                    Transaction.createEthCallTransaction(owner, CONTRACT_ADDRESS, FunctionEncoder.encode(function)),
                    DefaultBlockParameterName.LATEST)
                    .sendAsync().get();
        } catch (Exception e) {
            Log.e(TAG, "balanceOf(" + owner + "), error:" + e.getCause());
            return BigInteger.ZERO;
        }
        List<Type> someTypes = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        if (someTypes.size() == 0) return BigInteger.ZERO;
        Uint balance = (Uint) someTypes.get(0);
        return balance.getValue();
    }

    public RemoteCall<TransactionReceipt> approve() {
        String spender = ARPBank.CONTRACT_ADDRESS;
        BigInteger amount = Convert.toWei(APPROVE_ARP_NUMBER, Convert.Unit.ETHER).toBigInteger();
        Function function = getApproveFunction(spender, amount);
        return executeRemoteCallTransaction(function);
    }

    public static BigInteger estimateApproveGasLimit() {
        String spender = ARPBank.CONTRACT_ADDRESS;
        BigInteger amount = Convert.toWei(APPROVE_ARP_NUMBER, Convert.Unit.ETHER).toBigInteger();
        String functionString = FunctionEncoder.encode(getApproveFunction(spender, amount));
        return TransactionAPI.estimateFunctionGasLimit(functionString, CONTRACT_ADDRESS);
    }

    private static Function getBalanceOfFunction(String address) {
        return new Function(FUNC_BALANCE_OF,
                Arrays.<Type>asList(new Address(address)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint>() {
                }));
    }

    private static Function getAllowanceFunction(String owner, String spender) {
        return new Function(FUNC_ALLOWANCE,
                Arrays.<Type>asList(new Address(owner), new Address(spender)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint>() {
                }));
    }

    private static Function getApproveFunction(String spenderAddress, BigInteger value) {
        return new Function(FUNC_APPROVE,
                Arrays.<Type>asList(new Address(spenderAddress), new Uint(value)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {
                }));
    }
}
