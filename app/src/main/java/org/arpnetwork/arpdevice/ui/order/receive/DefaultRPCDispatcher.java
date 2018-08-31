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
import android.text.TextUtils;
import android.util.Log;

import org.arpnetwork.arpdevice.app.AppManager;
import org.arpnetwork.arpdevice.contracts.api.VerifyAPI;
import org.arpnetwork.arpdevice.data.DApp;
import org.arpnetwork.arpdevice.server.http.rpc.RPCDispatcher;
import org.arpnetwork.arpdevice.server.http.rpc.RPCErrorCode;
import org.arpnetwork.arpdevice.server.http.rpc.RPCRequest;
import org.arpnetwork.arpdevice.server.http.rpc.RPCResponse;
import org.arpnetwork.arpdevice.ui.bean.Miner;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.util.SignUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DefaultRPCDispatcher extends RPCDispatcher {
    private static final String TAG = DefaultRPCDispatcher.class.getSimpleName();

    private Context mContext;
    private AppManager mAppManager;
    private PromiseHandler mPromiseHandler;
    private ConcurrentMap<String, BigInteger> mNonceMap;
    private Miner mMiner;

    public DefaultRPCDispatcher(Context context, Miner miner) {
        mContext = context;
        mNonceMap = new ConcurrentHashMap<>();
        mMiner = miner;
    }

    public void setAppManager(AppManager appManager) {
        mAppManager = appManager;
    }

    public void setPromiseHandler(PromiseHandler handler) {
        mPromiseHandler = handler;
    }

    @Override
    protected void doRequest(RPCRequest request, RPCResponse response) {
        DApp dApp = mAppManager.getDApp();
        if (!checkRemoteAddress(request, dApp)) {
            response.setError(request.getId(), RPCErrorCode.INVALID_REQUEST, "Invalid request");
            return;
        }

        String dappAddr = "";
        if (dApp != null) {
            dappAddr = dApp.address;
        }
        String walletAddr = Wallet.get().getAddress();

        String method = request.getMethod();
        if ("device_ping".equals(method)) {
            responseResult(response, request.getId(), null, null);
        } else if ("app_install".equals(method)) {
            String packageName = request.getString(0);
            String url = request.getString(1);
            int filesize = request.getInt(2);
            String md5 = request.getString(3);
            String nonce = request.getString(4);
            String sign = request.getString(5);
            String data = String.format(Locale.US, "%s:%s:%s:%d:%s:%s:%s", method, packageName, url, filesize, md5, nonce, walletAddr);

            if (verify(response, request.getId(), data, nonce, sign, dappAddr)) {
                mAppManager.appInstall(packageName, url, filesize, md5);
                responseResult(response, request.getId(), nonce, dappAddr);
            }
        } else if ("app_uninstall".equals(method)) {
            String packageName = request.getString(0);
            String nonce = request.getString(1);
            String sign = request.getString(2);
            String data = String.format("%s:%s:%s:%s", method, packageName, nonce, walletAddr);

            if (verify(response, request.getId(), data, nonce, sign, dappAddr)) {
                mAppManager.uninstallApp(packageName);
                responseResult(response, request.getId(), nonce, dappAddr);
            }
        } else if ("app_start".equals(method)) {
            String packageName = request.getString(0);
            String nonce = request.getString(1);
            String sign = request.getString(2);
            String data = String.format("%s:%s:%s:%s", method, packageName, nonce, walletAddr);

            if (verify(response, request.getId(), data, nonce, sign, dappAddr)) {
                mAppManager.startApp(packageName);
                responseResult(response, request.getId(), nonce, dappAddr);
            }
        } else if ("account_pay".equals(method)) {
            String promiseJson = request.getString(0);
            String nonce = request.getString(1);
            String sign = request.getString(2);
            String data = String.format("%s:%s:%s:%s", method, promiseJson, nonce, walletAddr);

            if (verify(response, request.getId(), data, nonce, sign, mMiner.getAddress())) {
                if (mPromiseHandler.processPromise(promiseJson)) {
                    responseResult(response, request.getId(), nonce, mMiner.getAddress());
                } else {
                    response.setError(request.getId(), RPCErrorCode.INVALID_PARAMS, "Invalid params");
                }
            }
        } else {
            response.setError(request.getId(), RPCErrorCode.METHOD_NOT_FOUND, "Method not found");
        }
    }

    private boolean checkRemoteAddress(RPCRequest request, DApp dApp) {
        String remoteAddress = request.getRemoteAddress();
        if (remoteAddress.equals(mMiner.getIpString()) || (dApp != null && remoteAddress.equals(dApp.ip))) {
            return true;
        }
        return false;
    }

    private boolean verify(RPCResponse response, String id, String data, String nonce, String sign, String address) {
        if (!VerifyAPI.verifySign(data, sign, address)) {
            response.setError(id, RPCErrorCode.INVALID_PARAMS, "Invalid sign");
            return false;
        }
        if (!verifyNonce(nonce, address)) {
            response.setError(id, RPCErrorCode.INVALID_PARAMS, "Invalid nonce");
            return false;
        }
        return true;
    }

    private boolean verifyNonce(String nonce, String address) {
        boolean res = false;
        BigInteger n = null;
        try {
            n = new BigInteger(Numeric.cleanHexPrefix(nonce), 16);
        } catch (Exception e) {
        }

        BigInteger localNonce = mNonceMap.get(address);
        if (localNonce == null) {
            localNonce = new BigInteger("-1");
        }
        if (n != null && n.compareTo(localNonce) > 0) {
            mNonceMap.put(address, n);
            res = true;
        }
        return res;
    }

    private void responseResult(RPCResponse response, String id, String nonce, String address) {
        try {
            JSONObject result = new JSONObject();
            if (!TextUtils.isEmpty(nonce)) {
                result.put("nonce", nonce);
                result.put("sign", SignUtil.sign(String.format("%s:%s", nonce, address)));
            }

            response.setId(id);
            response.setResult(result);
        } catch (JSONException e) {
            Log.e(TAG, "Response failed. e = " + e.toString());
        }
    }
}
