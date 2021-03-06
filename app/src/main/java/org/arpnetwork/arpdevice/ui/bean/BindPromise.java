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

import android.content.Context;

import org.arpnetwork.arpdevice.contracts.api.VerifyAPI;
import org.arpnetwork.arpdevice.util.Util;
import org.spongycastle.util.encoders.Hex;
import org.web3j.crypto.Sign;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.Serializable;
import java.math.BigDecimal;
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

    public Sign.SignatureData getSignatureData() {
        byte[] signatureDataBytes = Hex.decode(getPromiseSign());
        return VerifyAPI.getSignatureDataFromByte(signatureDataBytes);
    }

    public String getAmountRaw() {
        return amount;
    }

    /**
     * convert amount that is hexValue with 0x to BigInteger.
     *
     * @return BigInteger
     */
    public BigInteger getAmountBig() {
        return Numeric.toBigInt(amount);
    }

    public BigDecimal getAmountHumanic() {
        return Convert.fromWei(getAmountBig().toString(), Convert.Unit.ETHER);
    }

    public BigInteger getExpired() {
        return new BigInteger(String.valueOf(expired));
    }

    public String getExpiredHumanic(Context context) {
        return Util.getLongDurationString(context, expired);
    }

    public BigInteger getSignExpired() {
        return new BigInteger(String.valueOf(signExpired));
    }

    @Override
    public String toString() {
        return "BindPromise{" +
                "amount='" + amount + '\'' +
                ", expired=" + expired +
                ", signExpired=" + signExpired +
                ", promiseSign='" + promiseSign + '\'' +
                ", sign='" + sign + '\'' +
                '}';
    }
}
