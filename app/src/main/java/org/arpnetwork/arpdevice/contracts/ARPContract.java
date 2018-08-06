package org.arpnetwork.arpdevice.contracts;

import org.arpnetwork.arpdevice.contracts.tasks.ARPApproveTask;
import org.arpnetwork.arpdevice.contracts.tasks.OnValueResult;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.util.Arrays;

public class ARPContract extends Contract {
    public static final String CONTRACT_ADDRESS = "0x8d39dd6b431bfb065b51fea07b7ee75bef0b53f8";

    private ARPContract(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super("", CONTRACT_ADDRESS, web3j, credentials, gasPrice, gasLimit);
    }

    public static ARPContract load(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ARPContract(web3j, credentials, gasPrice, gasLimit);
    }

    public String getApproveFunctionData(String spenderAddress, BigInteger value) {
        Function function = new Function("approve",
                Arrays.<Type>asList(new Address(spenderAddress), new Uint(value)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        String encodedFunction = FunctionEncoder.encode(function);
        return encodedFunction;
    }

    public static void approveARP(String password, String gasPrice, OnValueResult onResult) {
        ARPApproveTask arpApproveTask = new ARPApproveTask(onResult);
        arpApproveTask.execute(password, gasPrice);
    }
}
