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

package org.arpnetwork.arpdevice.contracts.tasks;

import android.os.AsyncTask;

import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.contracts.api.EtherAPI;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class BankAllowanceTask extends AsyncTask<String, String, BankAllowanceTask.BankAllowance> {
    private OnValueResult<BankAllowance> onResult;

    public BankAllowanceTask(OnValueResult<BankAllowance> onValueResult) {
        onResult = onValueResult;
    }

    @Override
    protected BankAllowance doInBackground(String... param) {
        String owner = param[0];
        String spender = param[1];
        BankAllowance bankAllowance = null;
        try {
            bankAllowance = getBankAllowance(owner, spender);
        } catch (ExecutionException ignored) {
        } catch (InterruptedException ignored) {
        }

        if (bankAllowance == null) {
            return null;
        }

        return bankAllowance;
    }

    @Override
    protected void onPostExecute(BankAllowance result) {
        if (!isCancelled() && onResult != null) {
            onResult.onValueResult(result);
        }
    }

    public static BankAllowance getBankAllowance(String owner, String spender) throws ExecutionException, InterruptedException {
        Function function = new Function("allowance",
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(owner),
                        new org.web3j.abi.datatypes.Address(spender)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {},
                        new TypeReference<Address>() {}));
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = EtherAPI.getWeb3J().ethCall(
                Transaction.createEthCallTransaction(owner, ARPBank.CONTRACT_ADDRESS, encodedFunction),
                DefaultBlockParameterName.LATEST)
                .sendAsync().get();
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

    public static class BankAllowance {
        public BigInteger id;
        public BigInteger amount;
        public BigInteger paid;
        public BigInteger expired;
        public String proxy;

        @Override
        public String toString() {
            return "BankBalance{" +
                    "id=" + id +
                    ", amount=" + amount +
                    ", paid=" + paid +
                    ", expired=" + expired +
                    ", proxy='" + proxy + '\'' +
                    '}';
        }
    }
}
