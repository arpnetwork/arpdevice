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

public class RPCResponse {
    private static final String VERSION = "2.0";

    private JSONObject response;

    public RPCResponse() {
        response = new JSONObject();
        setVersion();
    }

    public void setId(String id) throws JSONException {
        response.put("id", id);
    }

    public void setResult(JSONObject result) throws JSONException {
        response.put("result", result);
    }

    public void setError(JSONObject error) throws JSONException {
        response.put("error", error);
    }

    public void setError(int code, String id, String message) {
        try {
            JSONObject error = new JSONObject();
            error.put("code", code);
            error.put("message", message);

            setId(id);
            setError(error);
        } catch (JSONException e) {
        }
    }

    public String getJSONString() {
        return response.toString();
    }

    private void setVersion() {
        try {
            response.put("jsonrpc", VERSION);
        } catch (JSONException e) {
        }
    }
}
