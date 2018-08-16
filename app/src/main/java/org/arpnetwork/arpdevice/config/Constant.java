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

public class Constant {
    public static final int CHECK_DEFAULT = -1;
    public static final int CHECK_OS = 1;
    public static final int CHECK_ADB = 2;
    public static final int CHECK_TCP = 3;
    public static final int CHECK_AUTH = 4;
    public static final int CHECK_ADB_SAFE = 5;
    public static final int CHECK_AUTH_SUCCESS = 6;

    // Defines a custom Intent action
    public static final String BROADCAST_ACTION = "org.arpnetwork.arpdevice.BROADCAST";
    // Defines the key for the status "extra" in an Intent
    public static final String EXTENDED_DATA_STATUS = "org.arpnetwork.arpdevice.STATUS";

    public static final String KEY_OP = "op_type";
    public static final String KEY_PASSWD = "passwd";
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_GASPRICE = "gas_price";
    public static final String KEY_GASLIMIT = "gas_limit";
    public static final String KEY_BINDPROMISE = "bind_promise";

    public static final String ORDER_PRICE = "order_price";
}
