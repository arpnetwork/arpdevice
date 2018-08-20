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

package org.arpnetwork.arpdevice.util;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OKHttpUtils {
    private static final MediaType JSON = MediaType.parse("application/json");
    private static OkHttpClient mOkHttpClient;

    private final Gson mGson = new Gson();
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private static synchronized OkHttpClient getOkHttpClient() {
        if (mOkHttpClient == null) {
            mOkHttpClient = new OkHttpClient.Builder().
                    connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
        return mOkHttpClient;
    }

    enum Method {
        GET,
        POST,
    }

    public void get(String url, BaseCallback callback) {
        get(url, null, callback);
    }

    public void get(String url, String tag, BaseCallback callback) {
        get(url, tag, null, callback);
    }

    public void get(String url, String tag, Map<String, Object> param, BaseCallback callback) {
        Request request = buildRequest(url, Method.GET, tag, param);

        request(request, callback);
    }

    public void post(String url, String json, String tag, BaseCallback callback) {
        byte[] bytes = json.getBytes(); // Overwrite Content-Type: https://github.com/square/okhttp/issues/2099
        RequestBody body = RequestBody.create(JSON, bytes);
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body)
                .tag(tag);
        Request request = builder.build();
        request(request, callback);
    }

    public void post(String url, String tag, Map<String, Object> param, BaseCallback callback) {
        Request request = buildRequest(url, Method.POST, tag, param);
        request(request, callback);
    }

    public void cancelTag(Object tag) {
        if (tag == null) return;
        for (Call call : getOkHttpClient().dispatcher().queuedCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
        for (Call call : getOkHttpClient().dispatcher().runningCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
    }

    public void cancelAll() {
        for (Call call : getOkHttpClient().dispatcher().queuedCalls()) {
            call.cancel();
        }
        for (Call call : getOkHttpClient().dispatcher().runningCalls()) {
            call.cancel();
        }
    }

    private Request buildRequest(String url, Method methodType, String tag, Map<String, Object> params) {
        Request.Builder builder = new Request.Builder()
                .url(url);

        if (methodType == Method.POST) {
            RequestBody body = builderFormData(params);
            builder.post(body);
        } else if (methodType == Method.GET) {
            url = buildUrlParams(url, params);
            builder.url(url);
            builder.get();
        }
        builder.tag(tag);
        return builder.build();
    }

    private RequestBody builderFormData(Map<String, Object> params) {
        FormBody.Builder builder = new FormBody.Builder();

        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                builder.add(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString());
            }
        }
        return builder.build();
    }

    private String buildUrlParams(String url, Map<String, Object> params) {
        if (params == null)
            params = new HashMap<>(1);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            sb.append(entry.getKey() + "=" + (entry.getValue() == null ? "" : entry.getValue().toString()));
            sb.append("&");
        }
        String s = sb.toString();
        if (s.endsWith("&")) {
            s = s.substring(0, s.length() - 1);
        }

        if (url.indexOf("?") > 0) {
            url = url + "&" + s;
        } else {
            url = url + "?" + s;
        }

        return url;
    }

    void request(final Request request, final BaseCallback callback) {
        if (callback != null) {
            callback.onBeforeRequest(request);
        }

        getOkHttpClient().newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                if (callback == null) return;
                callbackFailure(callback, request, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (callback == null) return;
                callbackResponse(callback, response);

                if (response.isSuccessful()) {
                    String resultStr = response.body().string();

                    if (callback.mType == String.class) {
                        callbackSuccess(callback, response, resultStr);
                    } else {
                        boolean jsonRpcFormat = false;
                        JSONObject jsonRpc = null;
                        try {
                            jsonRpc = new JSONObject(resultStr);
                            String jsonRpcVersion = jsonRpc.getString("jsonrpc");
                            if (jsonRpcVersion.equals("2.0")) {
                                jsonRpcFormat = true;
                            }
                        } catch (JSONException e) {
                        }
                        if (jsonRpcFormat) {
                            if (jsonRpc.has("error")) {
                                callbackError(callback, response, null);
                                return;
                            }
                            resultStr = jsonRpc.optString("result");
                        }

                        try {
                            Object obj = mGson.fromJson(resultStr, callback.mType);
                            callbackSuccess(callback, response, obj);
                        } catch (com.google.gson.JsonParseException e) {
                            callbackError(callback, response, e);
                        }
                    }
                } else {
                    callbackError(callback, response, null);
                }
            }
        });
    }

    private void callbackSuccess(final BaseCallback callback, final Response response, final Object obj) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(response, obj);
            }
        });
    }

    private void callbackError(final BaseCallback callback, final Response response, final Exception e) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(response, response.code(), e);
            }
        });
    }

    private void callbackFailure(final BaseCallback callback, final Request request, final IOException e) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(request, e);
            }
        });
    }

    private void callbackResponse(final BaseCallback callback, final Response response) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onResponse(response);
            }
        });
    }
}