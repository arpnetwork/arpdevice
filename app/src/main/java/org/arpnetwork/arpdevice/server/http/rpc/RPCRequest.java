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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RPCRequest {
    private String id;
    private String method;
    private JSONArray paramsObj;

    public RPCRequest() {
    }

    public RPCRequest(String json) throws JSONException {
        parseJSON(json);
    }

    public String getId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    public String getString(int index) {
        String value = null;
        try {
            if (paramsObj != null) {
                value = paramsObj.getString(index);
            }
        } catch (JSONException e) {
        }
        return value;
    }

    public int getInt(int index) {
        int value = 0;
        try {
            if (paramsObj != null) {
                value = paramsObj.getInt(index);
            }
        } catch (JSONException e) {
        }
        return value;
    }

    private void parseJSON(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);

        if (jsonObject.has("id")) {
            id = jsonObject.getString("id");
        }

        if (jsonObject.has("method")) {
            method = jsonObject.getString("method");
        }

        if (jsonObject.has("params")) {
            paramsObj = jsonObject.getJSONArray("params");
        }
    }
}
