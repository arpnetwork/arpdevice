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

import org.arpnetwork.arpdevice.util.Util;

import java.math.BigInteger;

public class Miner {
    public String address;
    public BigInteger ip;
    public BigInteger port;
    public BigInteger size;
    public BigInteger expired;
    public BigInteger amount;

    public String name;
    public String country;
    public String bandwidth;
    public String load;

    public String getIpString() {
        return ip == null ? null : Util.longToIp(ip.longValue());
    }

    public int getPortHttpInt() {
        return port.intValue() + 1;
    }

    public int getPortTcpInt() {
        return port.intValue();
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Miner))
            return false;
        Miner pn = (Miner) o;
        return pn.address.equals(address);
    }

    @Override
    public String toString() {
        return "Miner{" +
                "address='" + address + '\'' +
                ", ip=" + getIpString() +
                ", port=" + port +
                ", size=" + size +
                ", expired=" + expired +
                ", amount=" + amount +
                ", name='" + name + '\'' +
                ", country='" + country + '\'' +
                ", bandwidth='" + bandwidth + '\'' +
                ", load='" + load + '\'' +
                '}';
    }
}
