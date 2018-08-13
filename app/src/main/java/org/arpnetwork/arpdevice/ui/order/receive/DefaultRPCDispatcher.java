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

package org.arpnetwork.arpdevice.ui.order.receive;

import android.content.Context;
import android.util.Log;

import org.arpnetwork.arpdevice.app.AppManager;
import org.arpnetwork.arpdevice.contracts.api.VerifyAPI;
import org.arpnetwork.arpdevice.server.http.rpc.RPCDispatcher;
import org.arpnetwork.arpdevice.server.http.rpc.RPCErrorCode;
import org.arpnetwork.arpdevice.server.http.rpc.RPCRequest;
import org.arpnetwork.arpdevice.server.http.rpc.RPCResponse;
import org.arpnetwork.arpdevice.util.SignUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.utils.Numeric;

public class DefaultRPCDispatcher extends RPCDispatcher {
    private static final String TAG = DefaultRPCDispatcher.class.getSimpleName();

    private Context mContext;
    private String mDAppAddress;
    private int mNonce;

    public DefaultRPCDispatcher(Context context) {
        mContext = context;
        mNonce = -1;
    }

    public void setDAppAddress(String address) {
        mDAppAddress = address;
    }

    @Override
    protected void doRequest(RPCRequest request, RPCResponse response) {
        if (mDAppAddress == null) {
            response.setError(RPCErrorCode.INVALID_REQUEST, request.getId(), "Invalid request");
            return;
        }

        String method = request.getMethod();
        if ("app_install".equals(method)) {
            String packageName = request.getString(0);
            String url = request.getString(1);
            int filesize = request.getInt(2);
            String md5 = request.getString(3);
            String nonce = request.getString(4);
            String sign = request.getString(5);
            String data = String.format("%s:%s:%s:%d:%s:%s:%s", method, packageName, url, filesize, md5, nonce, mDAppAddress);

            if (verify(response, request.getId(), data, sign, nonce)) {
                AppManager.appInstall(mContext, packageName, url, filesize, md5);
                responseResult(response, request.getId(), nonce);
            }
        } else if ("app_uninstall".equals(method)) {
            String packageName = request.getString(0);
            String nonce = request.getString(1);
            String sign = request.getString(2);
            String data = String.format("%s:%s:%s:%s", method, packageName, nonce, mDAppAddress);

            if (verify(response, request.getId(), data, sign, nonce)) {
                AppManager.uninstallApp(packageName);
                responseResult(response, request.getId(), nonce);
            }
        } else if ("app_start".equals(method)) {
            String packageName = request.getString(0);
            String nonce = request.getString(1);
            String sign = request.getString(2);
            String data = String.format("%s:%s:%s:%s", method, packageName, nonce, mDAppAddress);

            if (verify(response, request.getId(), data, sign, nonce)) {
                AppManager.startApp(packageName);
                responseResult(response, request.getId(), nonce);
            }
        } else if ("account_pay".equals(method)) {
            String promise = request.getString(0);
            String nonce = request.getString(1);
            String sign = request.getString(2);
            String data = String.format("%s:%s:%s:%s", method, promise, nonce, mDAppAddress);

            if (verify(response, request.getId(), data, sign, nonce)) {
                //FIXME: Receive a proof of token from dapp

                responseResult(response, request.getId(), nonce);
            }
        } else {
            response.setError(RPCErrorCode.METHOD_NOT_FOUND, request.getId(), "Method not found");
        }
    }

    private boolean verify(RPCResponse response, String id, String data, String sign, String nonce) {
        if (!verifySign(data, sign)) {
            response.setError(RPCErrorCode.INVALID_PARAMS, id, "Invalid sign");
            return false;
        }
        if (!verifyNonce(nonce)) {
            response.setError(RPCErrorCode.INVALID_PARAMS, id, "Invalid nonce");
            return false;
        }
        return true;
    }

    private boolean verifyNonce(String nonce) {
        boolean res = false;
        int n = -1;
        try {
            n = Integer.parseInt(Numeric.cleanHexPrefix(nonce), 16);
        } catch (NumberFormatException e) {
        }
        if (n > mNonce) {
            mNonce = n;
            res = true;
        }
        return res;
    }

    private boolean verifySign(String data, String sign) {
        boolean res = false;
        try {
            String addr = VerifyAPI.getSignatureAddress(data, sign);
            if (Numeric.cleanHexPrefix(mDAppAddress).equalsIgnoreCase(addr)) {
                res = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Verify sign failed. e = " + e.toString());
        }
        return res;
    }

    private void responseResult(RPCResponse response, String id, String nonce) {
        try {
            JSONObject result = new JSONObject();
            result.put("nonce", nonce);
            result.put("sign", SignUtil.sign(String.format("%s:%s", nonce, mDAppAddress)));

            response.setId(id);
            response.setResult(result);
        } catch (JSONException e) {
            Log.e(TAG, "Response failed. e = " + e.toString());
        }
    }
}
