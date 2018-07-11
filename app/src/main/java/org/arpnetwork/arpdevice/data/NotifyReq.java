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

public class NotifyReq extends Req<NotifyReqData> {
    public static final int ID_CONNECTED = 4;
    public static final int ID_CONNECT_TIMEOUT = 5;
    public static final int ID_FINISH_USE = 6;

    public int result;

    public NotifyReq(int id, int result, String session) {
        if (id < ID_CONNECTED || id > ID_FINISH_USE) {
            throw new IllegalArgumentException("Id is illegal.");
        }
        this.id = id;
        this.result = result;
        this.data = new NotifyReqData(session);
    }
}
