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

import java.util.concurrent.ConcurrentHashMap;

public class StateHolder {
    public static final int STATE_APPROVE_RUNNING = 1;
    public static final int STATE_APPROVE_SUCCESS = 2;
    public static final int STATE_APPROVE_FAILED = 3;

    public static final int STATE_BIND_RUNNING = 4;
    public static final int STATE_BIND_SUCCESS = 5;
    public static final int STATE_BIND_FAILED = 6;

    public static final int STATE_UNBOUND_RUNNING = 7;
    public static final int STATE_UNBOUND_SUCCESS = 8;
    public static final int STATE_UNBOUND_FAILED = 9;

    private static ConcurrentHashMap<TaskInfo, Integer> sTaskStateMap = new ConcurrentHashMap<>(1);

    public static void setState(TaskInfo task, int status) {
        sTaskStateMap.put(task, status);
    }

    public static Integer getState(TaskInfo task) {
        return sTaskStateMap.get(task);
    }

    public static TaskInfo getTaskByState(int state) {
        TaskInfo key = null;

        for (TaskInfo getKey : sTaskStateMap.keySet()) {
            if (sTaskStateMap.get(getKey).equals(state)) {
                key = getKey;
            }
        }
        return key;
    }
}
