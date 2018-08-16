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
import org.arpnetwork.arpdevice.contracts.api.BalanceAPI;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class BankAllowanceTask extends AsyncTask<String, String, BigDecimal> {
    private OnValueResult<BigDecimal> onResult;

    public BankAllowanceTask(OnValueResult<BigDecimal> onValueResult) {
        onResult = onValueResult;
    }

    @Override
    protected BigDecimal doInBackground(String... param) {
        String owner = param[0];
        String spender = param[1];
        Uint balance = null;
        try {
            balance = getBankAllowance(owner, spender);
        } catch (ExecutionException ignored) {
        } catch (InterruptedException ignored) {
        }

        if (balance == null) {
            return null;
        }

        return Convert.fromWei(balance.getValue().toString(), Convert.Unit.ETHER);
    }

    @Override
    protected void onPostExecute(BigDecimal result) {
        if (!isCancelled() && onResult != null) {
            onResult.onValueResult(result);
        }
    }

    public static Uint getBankAllowance(String owner, String spender) throws ExecutionException, InterruptedException {
        Function function = new Function("allowance",
                Arrays.<Type>asList(new Address(owner), new Address(spender)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint>() {
                }));
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = BalanceAPI.getWeb3J().ethCall(
                Transaction.createEthCallTransaction(owner, ARPBank.CONTRACT_ADDRESS, encodedFunction),
                DefaultBlockParameterName.LATEST)
                .sendAsync().get();
        List<Type> someTypes = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        return (Uint) someTypes.get(0);
    }
}
