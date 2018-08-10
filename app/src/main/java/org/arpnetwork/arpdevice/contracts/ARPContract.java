package org.arpnetwork.arpdevice.contracts;

import org.arpnetwork.arpdevice.contracts.api.TransactionAPI;
import org.arpnetwork.arpdevice.contracts.tasks.ARPAllowanceTask;
import org.arpnetwork.arpdevice.contracts.tasks.BindMinerHelper;
import org.arpnetwork.arpdevice.contracts.tasks.OnValueResult;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.tx.Contract;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

public class ARPContract extends Contract {
    public static final String CONTRACT_ADDRESS = "0xBeB6fdF4ef6CEb975157be43cBE0047B248a8922";
    public static final String APPROVE_ARP_NUMBER = "5000";

    private ARPContract(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super("", CONTRACT_ADDRESS, web3j, credentials, gasPrice, gasLimit);
    }

    public static ARPContract load(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ARPContract(web3j, credentials, gasPrice, gasLimit);
    }

    public static String getApproveFunctionData(String spenderAddress, BigInteger value) {
        Function function = new Function("approve",
                Arrays.<Type>asList(new Address(spenderAddress), new Uint(value)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {
                }));
        String encodedFunction = FunctionEncoder.encode(function);
        return encodedFunction;
    }

    public static String getTransactionHexData(String address, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        String hexData = TransactionAPI.getRawTransaction(gasPrice, gasLimit, CONTRACT_ADDRESS,
                getApproveFunctionData(address, new BigInteger(Convert.toWei(APPROVE_ARP_NUMBER, Convert.Unit.ETHER).toString())), credentials);
        return hexData;
    }

    public static void allowanceARP(String owner, String spender, OnValueResult<BigDecimal> onResult) {
        ARPAllowanceTask arpAllowanceTask = new ARPAllowanceTask(onResult);
        arpAllowanceTask.execute(owner, spender);
    }

    public static Transaction getApproveEstimateGasTrans() {
        String ownerAddress = Wallet.get().getPublicKey();
        String spenderAddress = BindMinerHelper.CONTRACT_ADDRESS;
        BigInteger value = new BigInteger(Convert.toWei(APPROVE_ARP_NUMBER, Convert.Unit.ETHER).toString());
        String data = getApproveFunctionData(spenderAddress, value);

        return Transaction.createEthCallTransaction(ownerAddress, CONTRACT_ADDRESS, data);
    }
}
