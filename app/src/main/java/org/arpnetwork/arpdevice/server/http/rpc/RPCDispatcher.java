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

package org.arpnetwork.arpdevice.server.http.rpc;

import org.arpnetwork.arpdevice.server.http.Dispatcher;
import org.arpnetwork.arpdevice.server.http.Request;
import org.arpnetwork.arpdevice.server.http.Response;
import org.json.JSONException;

public abstract class RPCDispatcher extends Dispatcher {

    @Override
    protected void doRequest(Request request, Response response) {
        try {
            RPCResponse rpcResponse = new RPCResponse();
            doRequest(new RPCRequest(request.getContent()), rpcResponse);
            response.setContent(rpcResponse.getJSONString());
        } catch (JSONException e) {
        }
    }

    protected abstract void doRequest(RPCRequest request, RPCResponse response) throws JSONException;
}
