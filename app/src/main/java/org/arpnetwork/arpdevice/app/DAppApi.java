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

package org.arpnetwork.arpdevice.app;

import org.arpnetwork.arpdevice.contracts.api.VerifyAPI;
import org.arpnetwork.arpdevice.data.DApp;
import org.arpnetwork.arpdevice.server.http.rpc.RPCRequest;
import org.arpnetwork.arpdevice.ui.wallet.Wallet;
import org.arpnetwork.arpdevice.util.OKHttpUtils;
import org.arpnetwork.arpdevice.util.SignUtil;
import org.arpnetwork.arpdevice.util.SimpleCallback;
import org.json.JSONObject;
import org.web3j.utils.Numeric;

import java.util.Locale;

import okhttp3.Request;
import okhttp3.Response;

public class DAppApi {
    private static final String TAG = DAppApi.class.getSimpleName();

    private static final String METHOD_APP_INSTALL = "app_notifyInstall";
    private static final String METHOD_CLIENT_CONNECTED = "client_connected";
    private static final String METHOD_CLIENT_DISCONNECTED = "client_disconnected";

    public static void appInstalled(final String pkg, int result, final DApp dApp) {
        String nonce = AtomicNonce.getAndIncrement();

        RPCRequest request = new RPCRequest();
        request.setId(null);
        request.setMethod(METHOD_APP_INSTALL);
        request.putString(pkg);
        request.putInt(result);
        request.putString(nonce);

        String data = String.format(Locale.US, "%s:%s:%d:%s:%s", METHOD_APP_INSTALL, pkg, result, nonce, dApp.address);
        String sign = SignUtil.sign(data);
        request.putString(sign);

        String json = request.toJSON();
        String url = String.format(Locale.US, "http://%s:%d", dApp.ip, dApp.port);

        new OKHttpUtils().post(url, json, METHOD_APP_INSTALL, null);
    }

    public static void clientConnected(String session, final DApp dApp, final Runnable successRunnable, final Runnable failedRunnable) {
        String nonce = AtomicNonce.getAndIncrement();

        RPCRequest request = new RPCRequest();
        request.setId(nonce);
        request.setMethod(METHOD_CLIENT_CONNECTED);
        request.putString(session);
        request.putString(nonce);

        String data = String.format(Locale.US, "%s:%s:%s:%s", METHOD_CLIENT_CONNECTED, session, nonce, dApp.address);
        String sign = SignUtil.sign(data);
        request.putString(sign);

        String json = request.toJSON();
        String url = String.format(Locale.US, "http://%s:%d", dApp.ip, dApp.port);

        new OKHttpUtils().post(url, json, METHOD_CLIENT_CONNECTED, new SimpleCallback<String>() {
            @Override
            public void onSuccess(Response response, String result) {
                boolean success = false;
                try {
                    JSONObject object = new JSONObject(result);
                    JSONObject res = object.getJSONObject("result");
                    String nonce = res.getString("nonce");
                    String sign = res.getString("sign");
                    String data = String.format("%s:%s", nonce, Wallet.get().getPublicKey());
                    String addr = VerifyAPI.getSignatureAddress(data, sign);
                    if (addr != null && addr.equalsIgnoreCase(Numeric.cleanHexPrefix(dApp.address))) {
                        if (successRunnable != null) {
                            successRunnable.run();
                        }
                        success = true;
                    }
                } catch (Exception e) {
                }

                if (!success) {
                    if (failedRunnable != null) {
                        failedRunnable.run();
                    }
                }
            }

            @Override
            public void onFailure(Request request, Exception e) {
                if (failedRunnable != null) {
                    failedRunnable.run();
                }
            }

            @Override
            public void onError(Response response, int code, Exception e) {
                if (failedRunnable != null) {
                    failedRunnable.run();
                }
            }
        });
    }

    public static void clientDisconnected(String session, DApp dApp) {
        String nonce = AtomicNonce.getAndIncrement();

        RPCRequest request = new RPCRequest();
        request.setId(nonce);
        request.setMethod(METHOD_CLIENT_DISCONNECTED);
        request.putString(session);
        request.putString(nonce);

        String data = String.format(Locale.US, "%s:%s:%s:%s", METHOD_CLIENT_DISCONNECTED, session, nonce, dApp.address);
        String sign = SignUtil.sign(data);
        request.putString(sign);

        String json = request.toJSON();
        String url = String.format(Locale.US, "http://%s:%d", dApp.ip, dApp.port);

        new OKHttpUtils().post(url, json, METHOD_CLIENT_DISCONNECTED, null);
    }
}
