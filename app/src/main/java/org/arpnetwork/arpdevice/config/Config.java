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

    public static final String API_URL = "https://easy-mock.com/mock/5afea0ae6ba6060f61c231d1/example";
    public static final String API_SERVER_VOUVHER = "server_voucher";

    public static final int DATA_SERVER_PORT = 9000;
    public static final int HTTP_SERVER_PORT = 9001;

    public static final int ORDER_PRICE_LOW = 0;
    public static final int ORDER_PRICE_HIGH = 100;
    public static final int ORDER_PRICE_DEFAULT = 1;
}
