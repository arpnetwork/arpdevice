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

import android.os.AsyncTask;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.contracts.api.TransactionAPI;
import org.arpnetwork.arpdevice.contracts.api.VerifyAPI;
import org.arpnetwork.arpdevice.contracts.tasks.BankAllowanceTask;
import org.arpnetwork.arpdevice.contracts.tasks.BankBalanceTask;
import org.arpnetwork.arpdevice.contracts.tasks.OnValueResult;
import org.arpnetwork.arpdevice.contracts.tasks.TransactionTask;
import org.arpnetwork.arpdevice.contracts.tasks.TransactionTask2;
import org.arpnetwork.arpdevice.data.BankAllowance;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;

import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;


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
    public static final String CONTRACT_ADDRESS = "0x19ea440d8a78a06be54ffca6a8564197bd1b443a";
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

    public RemoteCall<TransactionReceipt> deposit(BigInteger _value) {
        return executeRemoteCallTransaction(getDepositFunction(_value));
    }

    public RemoteCall<TransactionReceipt> cancelApprovalBySpender(String owner) {
        return executeRemoteCallTransaction(getCancelApprovalBySpenderFunction(owner));
    }

    public static String getApproveTransactionHexData(String spender, BigInteger expired, String proxy, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return getTransactionHexData(getApproveFunctionData(spender,
                new BigInteger(Convert.toWei(APPROVE_ARP_NUMBER, Convert.Unit.ETHER).toString()), expired, proxy),
                credentials, gasPrice, gasLimit);
    }

    public static void allowanceARP(String owner, String spender, OnValueResult<BankAllowance> onResult) {
        BankAllowanceTask arpAllowanceTask = new BankAllowanceTask(onResult);
        arpAllowanceTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, owner, spender);
    }

    public static void balanceOf(String owner, OnValueResult<BigDecimal> onResult) {
        BankBalanceTask arpAllowanceTask = new BankBalanceTask(onResult);
        arpAllowanceTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, owner);
    }

    public static Transaction getApproveEstimateGasTrans(String proxy) {
        String ownerAddress = Wallet.get().getAddress();
        String spenderAddress = ARPRegistry.CONTRACT_ADDRESS;
        BigInteger value = new BigInteger(Convert.toWei(APPROVE_ARP_NUMBER, Convert.Unit.ETHER).toString());
        String data = getApproveFunctionData(spenderAddress, value, BigInteger.ZERO, proxy);

        return Transaction.createEthCallTransaction(ownerAddress, CONTRACT_ADDRESS, data);
    }

    public static BigInteger estimateCashGasLimit(String owner, String spender, BigInteger amount,
            Sign.SignatureData signatureData) {
        String cashFunctionString = getCashFunctionData(owner, spender, amount, signatureData);
        return TransactionAPI.estimateFunctionGasLimit(cashFunctionString, CONTRACT_ADDRESS);
    }

    public static BigInteger estimateWithdrawGasLimit(BigInteger amount) {
        String withdrawFunctionString = getWithdrawFunctionData(amount);
        return TransactionAPI.estimateFunctionGasLimit(withdrawFunctionString, CONTRACT_ADDRESS);
    }

    public static void cash(Promise promise, String spender, Credentials credentials, BigInteger gasPrice,
            TransactionTask2.OnTransactionCallback<Boolean> onValueResult) {
        String owner = promise.getFrom();
        BigInteger amount = new BigInteger(promise.getAmount(), 16);
        Sign.SignatureData signatureData = VerifyAPI.getSignatureDataFromHexString(promise.getSign());
        BigInteger gasLimit = estimateCashGasLimit(owner, spender, amount, signatureData);
        String cashFunctionString = getCashFunctionData(Numeric.cleanHexPrefix(owner),
                spender, new BigInteger(promise.getAmount(), 16),
                VerifyAPI.getSignatureDataFromHexString(promise.getSign()));
        String transactionString = getTransactionHexData(cashFunctionString, credentials, gasPrice, gasLimit);
        TransactionTask2 task = new TransactionTask2(onValueResult);
        task.execute(transactionString);
    }

    public static BigInteger estimateDepositGasLimit(BigInteger value) {
        String depositFunctionString = FunctionEncoder.encode(getDepositFunction(value));
        return TransactionAPI.estimateFunctionGasLimit(depositFunctionString, CONTRACT_ADDRESS);
    }

    public static BigInteger estimateCancelApprovalBySpenderGasLimit(String owner) {
        String cancelApprovalBySpenderString = FunctionEncoder.encode(getCancelApprovalBySpenderFunction(owner));
        return TransactionAPI.estimateFunctionGasLimit(cancelApprovalBySpenderString, CONTRACT_ADDRESS);
    }

    public static void withdrawAll(final Credentials credentials, final BigInteger gasPrice,
            final OnValueResult<Boolean> onValueResult) {
        balanceOf(Wallet.get().getAddress(), new OnValueResult<BigDecimal>() {
            @Override
            public void onValueResult(BigDecimal result) {
                BigInteger amount = Convert.toWei(result, Convert.Unit.ETHER).toBigInteger();
                withdraw(amount, credentials, gasPrice, onValueResult);
            }
        });
    }

    private static void sendTransaction(String functionString, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit,
            OnValueResult<Boolean> onValueResult) {
        String transactionString = getTransactionHexData(functionString, credentials, gasPrice, gasLimit);
        TransactionTask task = new TransactionTask(onValueResult);
        task.execute(transactionString);
    }

    private static void withdraw(final BigInteger amount, final Credentials credentials, final BigInteger gasPrice,
            final OnValueResult<Boolean> onValueResult) {
        String functionString = getWithdrawFunctionData(amount);
        BigInteger gasLimit = estimateWithdrawGasLimit(amount);
        sendTransaction(functionString, credentials, gasPrice, gasLimit, onValueResult);
    }

    private static String getApproveFunctionData(String spender, BigInteger amount, BigInteger expired, String proxy) {
        Function function = new Function(
                FUNC_APPROVE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(spender),
                        new Uint256(amount), new Uint256(expired), new Address(proxy)),
                Collections.<TypeReference<?>>emptyList());
        return FunctionEncoder.encode(function);
    }

    private static String getCashFunctionData(String owner, String spender, BigInteger amount, Sign.SignatureData signatureData) {
        Function function = new Function(FUNC_CASH,
                Arrays.<Type>asList(new Address(owner), new Address(spender), new Uint256(amount),
                        new Uint8(signatureData.getV()), new Bytes32(signatureData.getR()),
                        new Bytes32(signatureData.getS())),
                Collections.<TypeReference<?>>emptyList());
        return FunctionEncoder.encode(function);
    }

    private static Function getCancelApprovalBySpenderFunction(String owner) {
        return new Function(FUNC_CANCELAPPROVALBYSPENDER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(owner)),
                Collections.<TypeReference<?>>emptyList());
    }

    private static Function getDepositFunction(BigInteger value) {
        return new Function(FUNC_DEPOSIT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(value)),
                Collections.<TypeReference<?>>emptyList());
    }

    private static String getWithdrawFunctionData(BigInteger amount) {
        Function function = new Function(FUNC_WITHDRAW,
                Arrays.<Type>asList(new Uint256(amount)),
                Collections.<TypeReference<?>>emptyList());
        return FunctionEncoder.encode(function);
    }

    private static String getTransactionHexData(String data, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return TransactionAPI.getRawTransaction(gasPrice, gasLimit, CONTRACT_ADDRESS,
                data, credentials);
    }

    public static List getTransactionList(String address, BigInteger earliestBlockNumber) throws ExecutionException, InterruptedException {
        Event event = new Event("Cashing",
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }, new TypeReference<Address>() {
                }),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }, new TypeReference<Uint256>() {
                }));
        EthFilter ethFilter = new EthFilter(new DefaultBlockParameterNumber(earliestBlockNumber), DefaultBlockParameterName.LATEST, CONTRACT_ADDRESS);
        ethFilter.addSingleTopic(EventEncoder.encode(event));
        ethFilter.addNullTopic();
        Address spender = new Address(address);
        String optTopicAddress = "0x" + TypeEncoder.encode(spender);
        ethFilter.addOptionalTopics(optTopicAddress);

        EthLog ethLog = EtherAPI.getWeb3J().ethGetLogs(ethFilter).sendAsync().get();
        return ethLog.getLogs();
    }
}
