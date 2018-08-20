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

package org.arpnetwork.arpdevice.data;

import org.arpnetwork.arpdevice.config.Config;

import java.math.BigInteger;

public class DApp {
    public String address;
    public String ip;
    public int port;
    public BigInteger price;

    public BigInteger getUnitAmount() {
        return getAmount(Config.REQUEST_PAYMENT_INTERVAL);
    }

    public BigInteger getAmount(int second) {
        return price.multiply(new BigInteger(String.valueOf(second)))
                .divide(new BigInteger("3600"));
    }

    public boolean priceValid() {
        String p = String.format("%.0f", DeviceInfo.get().getPrice() * Math.pow(10, 18));
        BigInteger devicePrice = new BigInteger(p);
        return price != null && devicePrice.compareTo(price) <= 0;
    }
}