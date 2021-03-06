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

import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.miner.BindMinerHelper;

public class GetBoundTask extends AbsExceptionTask<String, String, Miner> {

    public GetBoundTask(OnValueResult<Miner> onValueResult) {
        super(onValueResult);
    }

    @Override
    public Miner onInBackground(String... param) throws Exception {
        String address = param[0];
        return BindMinerHelper.getBound(address);
    }

}
