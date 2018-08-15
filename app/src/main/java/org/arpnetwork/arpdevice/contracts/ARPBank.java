package org.arpnetwork.arpdevice.contracts;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.arpnetwork.arpdevice.contracts.api.TransactionAPI;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple5;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.utils.Convert;

import rx.Observable;
import rx.functions.Func1;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 * <p>
 * <p>Generated with web3j version 3.5.0.
 */
public class ARPBank extends Contract {
    public static final String CONTRACT_ADDRESS = "0x0bebeedee8ebb75847515efde978c92596366b5d";
    private static final String BINARY = null;

    public static final String APPROVE_ARP_NUMBER = "500";

    public static final String FUNC_ARPTOKEN = "arpToken";

    public static final String FUNC_PERMANENT = "PERMANENT";

    public static final String FUNC_DEPOSIT = "deposit";

    public static final String FUNC_WITHDRAW = "withdraw";

    public static final String FUNC_APPROVE = "approve";

    public static final String FUNC_APPROVEBYPROXY = "approveByProxy";

    public static final String FUNC_INCREASEAPPROVAL = "increaseApproval";

    public static final String FUNC_DECREASEAPPROVAL = "decreaseApproval";

    public static final String FUNC_CANCELAPPROVAL = "cancelApproval";

    public static final String FUNC_CANCELAPPROVALBYSPENDER = "cancelApprovalBySpender";

    public static final String FUNC_CANCELAPPROVALBYPROXY = "cancelApprovalByProxy";

    public static final String FUNC_CASH = "cash";

    public static final String FUNC_BALANCEOF = "balanceOf";

    public static final String FUNC_ALLOWANCE = "allowance";

    protected ARPBank(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ARPBank(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ARPBank load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ARPBank(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    public static ARPBank load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ARPBank(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static String getApproveFunctionData(String spender, BigInteger amount, BigInteger expired, String proxy) {
        Function function = new Function(
                FUNC_APPROVE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(spender),
                        new org.web3j.abi.datatypes.generated.Uint256(amount),
                        new org.web3j.abi.datatypes.generated.Uint256(expired),
                        new org.web3j.abi.datatypes.Address(proxy)),
                Collections.<TypeReference<?>>emptyList());
        return FunctionEncoder.encode(function);
    }

    public static String getTransactionHexData(String spender, BigInteger expired, String proxy, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        String hexData = TransactionAPI.getRawTransaction(gasPrice, gasLimit, CONTRACT_ADDRESS,
                getApproveFunctionData(spender, new BigInteger(Convert.toWei(APPROVE_ARP_NUMBER, Convert.Unit.ETHER).toString()), expired, proxy), credentials);
        return hexData;
    }
}
