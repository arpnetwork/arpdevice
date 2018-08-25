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

package org.arpnetwork.arpdevice.ui.bean;

import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.Serializable;
import java.math.BigInteger;

public class BindPromise implements Serializable {
    private String amount; // hex
    private long expired;
    private long signExpired;
    private String promiseSign; // hex
    private String sign; // hex

    public String getSign() {
        return Numeric.cleanHexPrefix(sign);
    }

    public String getPromiseSign() {
        return Numeric.cleanHexPrefix(promiseSign);
    }

    public BigInteger getAmount() {
        return Numeric.toBigInt(amount);
    }

    public BigInteger getAmountHumanic() {
        return Convert.fromWei(getAmount().toString(), Convert.Unit.ETHER).toBigInteger();
    }

    public BigInteger getExpired() {
        return new BigInteger(String.valueOf(expired));
    }

    public BigInteger getSignExpired() {
        return new BigInteger(String.valueOf(signExpired));
    }

    @Override
    public String toString() {
        return "BindPromise{" +
                "amount='" + amount + '\'' +
                ", expired=" + expired +
                ", getSignExpired=" + signExpired +
                '}';
    }
}
