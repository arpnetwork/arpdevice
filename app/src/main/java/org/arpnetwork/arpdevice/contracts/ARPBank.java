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

import android.util.Log;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.arpnetwork.arpdevice.contracts.api.TransactionAPI;
import org.arpnetwork.arpdevice.contracts.api.VerifyAPI;
import org.arpnetwork.arpdevice.data.BankAllowance;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.miner.BindMinerHelper;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;

import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

public class ARPBank extends Contract {
    private static final String TAG = Contract.class.getSimpleName();
    public static final String CONTRACT_ADDRESS = "0x19ea440d8a78a06be54ffca6a8564197bd1b443a";

    public static final String APPROVE_ARP_NUMBER = "500";
    public static final String DEPOSIT_ARP_NUMBER = "500";

    public static final String FUNC_DEPOSIT = "deposit";
    public static final String FUNC_WITHDRAW = "withdraw";
    public static final String FUNC_APPROVE = "approve";
    public static final String FUNC_CANCEL_APPROVAL_BY_SPENDER = "cancelApprovalBySpender";
    public static final String FUNC_CASH = "cash";
    public static final String FUNC_BALANCE_OF = "balanceOf";
    public static final String FUNC_ALLOWANCE = "allowance";
    public static final String EVENT_CASHING = "Cashing";

    protected ARPBank(Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super("", CONTRACT_ADDRESS, EtherAPI.getWeb3J(), credentials, gasPrice, gasLimit);
    }

    protected ARPBank(TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super("", CONTRACT_ADDRESS, EtherAPI.getWeb3J(), transactionManager, gasPrice, gasLimit);
    }


    public static ARPBank load(Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ARPBank(credentials, gasPrice, gasLimit);
    }

    public static ARPBank load(TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ARPBank(transactionManager, gasPrice, gasLimit);
    }

    public static BankAllowance allowance(String owner, String spender) {
        Function function = getAllowanceFunction(owner, spender);
        EthCall response = null;
        try {
            response = EtherAPI.getWeb3J().ethCall(
                    Transaction.createEthCallTransaction(owner, ARPBank.CONTRACT_ADDRESS, FunctionEncoder.encode(function)),
                    DefaultBlockParameterName.LATEST)
                    .sendAsync().get();
        } catch (Exception e) {
            Log.e(TAG, "allowance(" + owner + ", " + spender + "), error:" + e.getCause());
            return null;
        }
        List<Type> results = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());

        BankAllowance bankBalance = new BankAllowance();
        bankBalance.id = (BigInteger) results.get(0).getValue();
        bankBalance.amount = (BigInteger) results.get(1).getValue();
        bankBalance.paid = (BigInteger) results.get(2).getValue();
        bankBalance.expired = (BigInteger) results.get(3).getValue();
        bankBalance.proxy = (String) results.get(4).getValue();
        return bankBalance;
    }

    public static BigInteger balanceOf(String owner) {
        Function function = getBalanceOfFunction(owner);
        EthCall response = null;
        try {
            response = EtherAPI.getWeb3J().ethCall(
                    Transaction.createEthCallTransaction(owner, ARPBank.CONTRACT_ADDRESS, FunctionEncoder.encode(function)),
                    DefaultBlockParameterName.LATEST)
                    .sendAsync().get();
        } catch (Exception e) {
            Log.e(TAG, "balanceOf(" + owner + "), error:" + e.getCause());
            return new BigInteger("0");
        }
        List<Type> someTypes = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        Uint balance = (Uint) someTypes.get(0);
        return balance.getValue();
    }

    public RemoteCall<TransactionReceipt> approve(String spender, BigInteger expired, String proxy) {
        Function function = getApproveFunction(spender,
                new BigInteger(Convert.toWei(APPROVE_ARP_NUMBER, Convert.Unit.ETHER).toString()), expired, proxy);
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> deposit(BigInteger value) {
        Function function = getDepositFunction(value);
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> withdrawAll() {
        BigInteger amount = balanceOf(Wallet.get().getAddress());
        return withdraw(amount);
    }

    public RemoteCall<TransactionReceipt> cash(Promise promise, String spender) {
        Function function = getCashFunction(Numeric.cleanHexPrefix(promise.getFrom()),
                spender, new BigInteger(promise.getAmount(), 16),
                VerifyAPI.getSignatureDataFromHexString(promise.getSign()));
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> cancelApprovalBySpender(String owner) {
        Function function = getCancelApprovalBySpenderFunction(owner);
        return executeRemoteCallTransaction(function);
    }

    // estimate gas limit

    public static BigInteger estimateApproveGasLimit(String spender) {
        String functionString = FunctionEncoder.encode(getApproveFunction(spender,
                new BigInteger(Convert.toWei(APPROVE_ARP_NUMBER, Convert.Unit.ETHER).toString()),
                new BigInteger("0"), ARPRegistry.CONTRACT_ADDRESS));
        return TransactionAPI.estimateFunctionGasLimit(functionString, CONTRACT_ADDRESS);
    }

    public static BigInteger estimateDepositGasLimit() {
        String depositFunctionString = FunctionEncoder.encode(
                getDepositFunction(Convert.toWei(APPROVE_ARP_NUMBER, Convert.Unit.ETHER).toBigInteger()));
        return TransactionAPI.estimateFunctionGasLimit(depositFunctionString, CONTRACT_ADDRESS);
    }

    public static BigInteger estimateWithdrawGasLimit(BigInteger amount) {
        String withdrawFunctionString = FunctionEncoder.encode(getWithdrawFunction(amount));
        return TransactionAPI.estimateFunctionGasLimit(withdrawFunctionString, CONTRACT_ADDRESS);
    }

    public static BigInteger estimateCashGasLimit(Promise promise, String spender) {
        String cashFunctionString = FunctionEncoder.encode(getCashFunction(Numeric.cleanHexPrefix(promise.getFrom()),
                spender, new BigInteger(promise.getAmount(), 16),
                VerifyAPI.getSignatureDataFromHexString(promise.getSign())));
        return TransactionAPI.estimateFunctionGasLimit(cashFunctionString, CONTRACT_ADDRESS);
    }

    public static BigInteger estimateCancelApprovalBySpenderGasLimit(String owner) {
        String cancelApprovalBySpenderString = FunctionEncoder.encode(getCancelApprovalBySpenderFunction(owner));
        return TransactionAPI.estimateFunctionGasLimit(cancelApprovalBySpenderString, CONTRACT_ADDRESS);
    }

    // event

    public static List getTransactionList(String address, BigInteger earliestBlockNumber) throws ExecutionException, InterruptedException {
        Event event = new Event(EVENT_CASHING,
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        EthFilter ethFilter = new EthFilter(new DefaultBlockParameterNumber(earliestBlockNumber), DefaultBlockParameterName.LATEST, CONTRACT_ADDRESS);
        ethFilter.addSingleTopic(EventEncoder.encode(event));
        ethFilter.addNullTopic();
        Address spender = new Address(address);
        String optTopicAddress = "0x" + TypeEncoder.encode(spender);
        ethFilter.addOptionalTopics(optTopicAddress);

        EthLog ethLog = EtherAPI.getWeb3J().ethGetLogs(ethFilter).sendAsync().get();
        return ethLog.getLogs();
    }

    public static BigInteger getUnexchange() throws Exception {
        if (Promise.get() != null) {
            final BigInteger amount = new BigInteger(Promise.get().getAmount(), 16);
            String spender = Wallet.get().getAddress();
            if (amount.compareTo(BigInteger.ZERO) > 0) {
                Miner miner = BindMinerHelper.getBound(spender);
                if (miner == null) {
                    throw new Exception("get miner error");
                }

                BankAllowance allowance = ARPBank.allowance(miner.getAddress(), spender);
                if (allowance == null) {
                    throw new Exception("get allowance error");
                } else {
                    return amount.subtract(allowance.paid);
                }
            }
        }
        return BigInteger.ZERO;
    }

    private RemoteCall<TransactionReceipt> withdraw(BigInteger amount) {
        Function function = getWithdrawFunction(amount);
        return executeRemoteCallTransaction(function);
    }

    private static Function getBalanceOfFunction(String owner) {
        return new Function(FUNC_BALANCE_OF,
                Arrays.<Type>asList(new Address(owner)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    }

    private static Function getAllowanceFunction(String owner, String spender) {
        return new Function(ARPBank.FUNC_ALLOWANCE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(owner),
                        new org.web3j.abi.datatypes.Address(spender)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {},
                        new TypeReference<Address>() {}));
    }

    private static Function getApproveFunction(String spender, BigInteger amount, BigInteger expired, String proxy) {
        return new Function(
                FUNC_APPROVE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(spender),
                        new Uint256(amount), new Uint256(expired), new Address(proxy)),
                Collections.<TypeReference<?>>emptyList());
    }

    private static Function getDepositFunction(BigInteger value) {
        return new Function(FUNC_DEPOSIT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(value)),
                Collections.<TypeReference<?>>emptyList());
    }

    private static Function getWithdrawFunction(BigInteger amount) {
        return new Function(FUNC_WITHDRAW,
                Arrays.<Type>asList(new Uint256(amount)),
                Collections.<TypeReference<?>>emptyList());
    }

    private static Function getCashFunction(String owner, String spender, BigInteger amount, Sign.SignatureData signatureData) {
        return new Function(FUNC_CASH,
                Arrays.<Type>asList(new Address(owner), new Address(spender), new Uint256(amount),
                        new Uint8(signatureData.getV()), new Bytes32(signatureData.getR()),
                        new Bytes32(signatureData.getS())),
                Collections.<TypeReference<?>>emptyList());
    }

    private static Function getCancelApprovalBySpenderFunction(String owner) {
        return new Function(FUNC_CANCEL_APPROVAL_BY_SPENDER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(owner)),
                Collections.<TypeReference<?>>emptyList());
    }
}
