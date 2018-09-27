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

package org.arpnetwork.arpdevice.config;

public class Config {
    public static final boolean DEBUG = false;

    public static final String PROTOCOL_VERSION = "1.0";
    public static final String DEFAULT_BLOCKNUMBER = "0";

    public static final String API_ETH_PRICE = "https://api.etherscan.io/api?module=stats&action=ethprice";
    public static final String API_GAS_PRICE = "https://ethgasstation.info/index.php";
    public static final String API_SERVER_INFO = "server_info";
    public static final String API_SERVER_BIND_PROMISE = "server_bindPromise";

    public static final int DATA_SERVER_PORT = 33333;
    public static final int HTTP_SERVER_PORT = 33334;

    public static final int ORDER_PRICE_LOW = 0;
    public static final int ORDER_PRICE_HIGH = 100;
    public static final int ORDER_PRICE_DEFAULT = 1;

    public static final int REQUEST_PAYMENT_INTERVAL = 9;
    public static final float FEE_PERCENT = 0.05f;

    public static final int MONITOR_MINER_INTERVAL = 1 * 60 * 60 * 1000;

    public static final int DEFAULT_POLLING_FREQUENCY = 15 * 1000; // 15s
    public static final int DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH = 240;
}
