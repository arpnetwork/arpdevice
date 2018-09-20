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

package org.arpnetwork.arpdevice.contracts.tasks;

import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.arpnetwork.arpdevice.data.BankAllowance;

import java.io.IOException;

public class BankAllowanceTask extends AbsExceptionTask<String, String, BankAllowance> {

    public BankAllowanceTask(OnValueResult<BankAllowance> onValueResult) {
        super(onValueResult);
    }

    @Override
    public BankAllowance onInBackground(String... param) throws IOException {
        String owner = param[0];
        String spender = param[1];
        return ARPBank.allowance(owner, spender);
    }
}
