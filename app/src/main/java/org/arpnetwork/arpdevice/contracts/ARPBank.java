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
import java.util.concurrent.Callable;

import org.arpnetwork.arpdevice.contracts.api.TransactionAPI;
import org.arpnetwork.arpdevice.contracts.api.VerifyAPI;
import org.arpnetwork.arpdevice.contracts.tasks.BankAllowanceTask;
import org.arpnetwork.arpdevice.contracts.tasks.BankBalanceTask;
import org.arpnetwork.arpdevice.contracts.tasks.OnValueResult;
import org.arpnetwork.arpdevice.contracts.tasks.TransactionGasEstimateTask;
import org.arpnetwork.arpdevice.contracts.tasks.TransactionTask;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;

import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple5;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.utils.Convert;


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

    public RemoteCall<TransactionReceipt> deposit(BigInteger _value) {
        final Function function = new Function(
                FUNC_DEPOSIT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_value)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> cancelApprovalBySpender(String owner) {
        final Function function = new Function(
                FUNC_CANCELAPPROVALBYSPENDER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(owner)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static String getApproveTransactionHexData(String spender, BigInteger expired, String proxy, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return getTransactionHexData(getApproveFunctionData(spender,
                new BigInteger(Convert.toWei(APPROVE_ARP_NUMBER, Convert.Unit.ETHER).toString()), expired, proxy),
                credentials, gasPrice, gasLimit);
    }

    public static void allowanceARP(String owner, String spender, OnValueResult<BankAllowanceTask.BankAllowance> onResult) {
        BankAllowanceTask arpAllowanceTask = new BankAllowanceTask(onResult);
        arpAllowanceTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, owner, spender);
    }

    public static void balanceOf(String owner, OnValueResult<BigDecimal> onResult) {
        BankBalanceTask arpAllowanceTask = new BankBalanceTask(onResult);
        arpAllowanceTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, owner);
    }

    public static Transaction getApproveEstimateGasTrans(String proxy) {
        String ownerAddress = Wallet.get().getPublicKey();
        String spenderAddress = ARPRegistry.CONTRACT_ADDRESS;
        BigInteger value = new BigInteger(Convert.toWei(APPROVE_ARP_NUMBER, Convert.Unit.ETHER).toString());
        String data = getApproveFunctionData(spenderAddress, value,BigInteger.ZERO,proxy);

        return Transaction.createEthCallTransaction(ownerAddress, CONTRACT_ADDRESS, data);
    }

    public static void estimateCashGasLimit(String from, BigInteger amount, Sign.SignatureData signatureData,
            final OnValueResult<BigInteger> onValueResult) {
        String cashFunctionString = getCashFunctionData(from, amount, signatureData);
        String ownerAddress = Wallet.get().getPublicKey();
        Transaction transaction = Transaction.createEthCallTransaction(ownerAddress, CONTRACT_ADDRESS, cashFunctionString);
        TransactionGasEstimateTask estimateTask = new TransactionGasEstimateTask(transaction, onValueResult);
        estimateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void estimateWithdrawGasLimit(BigInteger amount, final OnValueResult<BigInteger> onValueResult) {
        String withdrawFunctionString = getWithdrawFunctionData(amount);
        String ownerAddress = Wallet.get().getPublicKey();
        Transaction transaction = Transaction.createEthCallTransaction(ownerAddress, CONTRACT_ADDRESS, withdrawFunctionString);
        TransactionGasEstimateTask estimateTask = new TransactionGasEstimateTask(transaction, onValueResult);
        estimateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void cash(Promise promise, Credentials credentials, BigInteger gasPrice,
            BigInteger gasLimit, OnValueResult<Boolean> onValueResult) {
        String cashFunctionString = getCashFunctionData(promise.getFrom(), new BigInteger(promise.getAmount()),
                VerifyAPI.getSignatureDataFromHexString(promise.getSign()));
        String transactionString = getTransactionHexData(cashFunctionString, credentials, gasPrice, gasLimit);
        TransactionTask task = new TransactionTask(onValueResult);
        task.execute(transactionString);
    }

    public static void withdrawAll(final Credentials credentials, final BigInteger gasPrice,
            final OnValueResult<Boolean> onValueResult) {
        balanceOf(Wallet.get().getPublicKey(), new OnValueResult<BigDecimal>() {
            @Override
            public void onValueResult(BigDecimal result) {
                BigInteger amount = Convert.toWei(result, Convert.Unit.ETHER).toBigInteger();
                withdraw(amount, credentials, gasPrice, onValueResult);
            }
        });
    }

    private static void withdraw(final BigInteger amount, final Credentials credentials, final BigInteger gasPrice,
            final OnValueResult<Boolean> onValueResult) {
        estimateWithdrawGasLimit(amount, new OnValueResult<BigInteger>() {
            @Override
            public void onValueResult(BigInteger result) {
                String withdrawFunctionString = getWithdrawFunctionData(amount);
                String transactionString;
                transactionString = getTransactionHexData(withdrawFunctionString, credentials, gasPrice, result);
                TransactionTask task = new TransactionTask(onValueResult);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, transactionString);
            }
        });
    }

    private static String getApproveFunctionData(String spender, BigInteger amount, BigInteger expired, String proxy) {
        Function function = new Function(
                FUNC_APPROVE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(spender),
                        new Uint256(amount), new Uint256(expired), new Address(proxy)),
                Collections.<TypeReference<?>>emptyList());
        return FunctionEncoder.encode(function);
    }

    private static String getCashFunctionData(String from, BigInteger amount, Sign.SignatureData signatureData) {
        Function function = new Function(FUNC_CASH,
                Arrays.<Type>asList(new Address(from), new Uint256(amount),
                        new Uint8(signatureData.getV()), new Bytes32(signatureData.getR()),
                        new Bytes32(signatureData.getS())),
                Collections.<TypeReference<?>>emptyList());
        return FunctionEncoder.encode(function);
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
}
