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
    private String address;
    private BigInteger ip;
    private BigInteger port;
    private BigInteger size;
    private BigInteger expired;

    private MinerInfo minerInfo;

    public void setAddress(String address) {
        this.address = address;
    }

    public void setIp(BigInteger ip) {
        this.ip = ip;
    }

    public void setSize(BigInteger size) {
        this.size = size;
    }

    public void setPort(BigInteger port) {
        this.port = port;
    }

    public void setExpired(BigInteger expired) {
        this.expired = expired;
    }

    public void setMinerInfo(MinerInfo minerInfo) {
        this.minerInfo = minerInfo;
    }

    public String getAddress() {
        return address;
    }

    public String getIpString() {
        return ip == null ? null : Util.longToIp(ip.longValue());
    }

    public int getPortHttpInt() {
        return port.intValue() + 1;
    }

    public int getPortTcpInt() {
        return port.intValue();
    }

    public BigInteger getSize() {
        return size;
    }

    public BigInteger getExpired() {
        return expired;
    }

    public MinerInfo getMinerInfo() {
        return minerInfo;
    }

    public boolean expiredValid() {
        BigInteger nextDay = new BigInteger(String.valueOf(System.currentTimeMillis() / 1000 + 24 * 60 * 60));
        return expired != null && (expired.compareTo(BigInteger.ZERO) == 0 || expired.compareTo(nextDay) >= 0);
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
}
