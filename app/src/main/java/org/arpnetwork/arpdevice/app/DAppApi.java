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

import org.arpnetwork.arpdevice.data.DApp;
import org.arpnetwork.arpdevice.server.http.rpc.RPCRequest;
import org.arpnetwork.arpdevice.util.OKHttpUtils;
import org.arpnetwork.arpdevice.util.SignUtil;
import org.arpnetwork.arpdevice.util.SimpleCallback;

import okhttp3.Request;
import okhttp3.Response;

public class DAppApi {
    private static final String TAG = DAppApi.class.getSimpleName();

    public static void appInstalled(final String pkg, int result, final DApp dApp) {
        String nonce = AtomicNonce.getAndIncrement();

        RPCRequest request = new RPCRequest();
        request.setId(null);
        request.setMethod("app_notifyInstall");
        request.putString(pkg);
        request.putInt(result);
        request.putString(nonce);

        String data = String.format("app_notifyInstall:%s:%d:%s:%s", pkg, result, nonce, dApp.address);
        String sign = SignUtil.sign(data);
        request.putString(sign);

        String json = request.toJSON();
        String url = String.format("http://%s:%d", dApp.ip, dApp.port);

        new OKHttpUtils().post(url, json, "appInstalled", null);
    }

    public static void clientConnected(String session, DApp dApp, final Runnable successRunnable, final Runnable failedRunnable) {
        String nonce = AtomicNonce.getAndIncrement();

        RPCRequest request = new RPCRequest();
        request.setId(nonce);
        request.setMethod("client_connected");
        request.putString(session);
        request.putString(nonce);

        String data = String.format("client_connected:%s:%s:%s", session, nonce, dApp.address);
        String sign = SignUtil.sign(data);
        request.putString(sign);

        String json = request.toJSON();
        String url = String.format("http://%s:%d", dApp.ip, dApp.port);

        new OKHttpUtils().post(url, json, "clientConnected", new SimpleCallback<String>() {
            @Override
            public void onSuccess(Response response, String result) {
                if (successRunnable != null) {
                    successRunnable.run();
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
        request.setMethod("client_disconnected");
        request.putString(session);
        request.putString(nonce);

        String data = String.format("client_disconnected:%s:%s:%s", session, nonce, dApp.address);
        String sign = SignUtil.sign(data);
        request.putString(sign);

        String json = request.toJSON();
        String url = String.format("http://%s:%d", dApp.ip, dApp.port);

        new OKHttpUtils().post(url, json, "clientDisconnected", null);
    }
}
