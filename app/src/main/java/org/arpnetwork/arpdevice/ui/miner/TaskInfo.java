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

package org.arpnetwork.arpdevice.ui.miner;

public class TaskInfo {
    public int opType;
    public String address;

    public TaskInfo(int opType, String address) {
        this.opType = opType;
        this.address = address;
    }

    @Override
    public int hashCode() {
        return opType + (address == null ? 0 : address.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TaskInfo))
            return false;
        TaskInfo pn = (TaskInfo) o;

        if (pn.address != null) {
            return pn.opType == opType && pn.address.equals(address);
        }

        return pn.opType == opType;
    }
}
