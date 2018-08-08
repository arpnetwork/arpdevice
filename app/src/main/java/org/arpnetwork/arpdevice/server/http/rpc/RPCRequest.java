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

import org.json.JSONException;
import org.json.JSONObject;

public class RPCRequest {
    private int id;
    private String method;
    private JSONObject paramsObj;

    public RPCRequest() {
    }

    public RPCRequest(String json) throws JSONException {
        parseJSON(json);
    }

    public int getId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    public boolean hasParam(String param) {
        return paramsObj != null && paramsObj.has(param);
    }

    public String getParam(String param) throws JSONException {
        return paramsObj != null ? paramsObj.getString(param) : null;
    }

    private void parseJSON(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);

        if (jsonObject.has("id")) {
            id = jsonObject.getInt("id");
        }

        if (jsonObject.has("method")) {
            method = jsonObject.getString("method");
        }

        if (jsonObject.has("params")) {
            paramsObj = jsonObject.getJSONObject("params");
        }
    }
}
