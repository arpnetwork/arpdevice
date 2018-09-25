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

import android.text.TextUtils;

import org.arpnetwork.arpdevice.config.Config;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Request;
import okhttp3.Response;

public class PriceProvider {
    private static final String TAG = "PriceProvider";
    private static final String DEFAULT_USD_YUAN = "6.86";
    private static final String DEFAULT_GAS_GWEI = "20";
    private static final String DEFAULT_ETH_USD = "210";
    private static final String KEY_PRICE_INFO = "key_price_info";
    private static final long EXP_MS = 1 * 60 * 60 * 1000;

    public static class PriceInfo {
        private String gasPriceGwei;
        private String ethToYuanRate;
        private long timestamp;

        public String getEthToYuanRate() {
            return ethToYuanRate;
        }

        public String getGasPriceGwei() {
            return gasPriceGwei;
        }

        @Override
        public String toString() {
            return "PriceInfo{" +
                    "gasPriceGwei='" + gasPriceGwei + '\'' +
                    ", ethToYuanRate='" + ethToYuanRate + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    public interface Callback {
        void onPriceInfo(PriceInfo priceInfo);
    }

    public static void initOrLoadPrice(final Callback callback) {
        // load from cache
        // cache valid 1 hour
        // load from network and save or update
        final String priceJson = PreferenceManager.getInstance().getString(KEY_PRICE_INFO);
        PriceInfo cachedPrice;
        if (!TextUtils.isEmpty(priceJson)) {
            cachedPrice = Util.loadObject(priceJson, PriceInfo.class);
            if (cachedPrice != null && System.currentTimeMillis() - cachedPrice.timestamp <= EXP_MS) {
                if (callback != null) {
                    callback.onPriceInfo(cachedPrice);
                }
                return;
            }
        }

        final OKHttpUtils okHttpUtils = new OKHttpUtils();
        final PriceInfo priceInfo = new PriceInfo();
        okHttpUtils.get(Config.API_GAS_PRICE, new SimpleCallback<String>() {
            @Override
            public void onFailure(Request request, Exception e) {
                priceInfo.gasPriceGwei = DEFAULT_GAS_GWEI;
                loadEthusd();
            }

            @Override
            public void onSuccess(Response response, String result) {
                Pattern pattern = Pattern.compile("Gas Price SafeLow \\(Gwei\\)</span>$\\s*?<div class=\"count green\">(.*?)<\\/div>", Pattern.MULTILINE);
                Matcher matcher = pattern.matcher(result);
                if (matcher.find()) {
                    priceInfo.gasPriceGwei = matcher.group(1);
                } else {
                    priceInfo.gasPriceGwei = DEFAULT_GAS_GWEI;
                }
                loadEthusd();
            }

            private void loadEthusd() {
                okHttpUtils.get(Config.API_ETH_PRICE, new SimpleCallback<String>() {
                    @Override
                    public void onFailure(Request request, Exception e) {
                        handleCallback(DEFAULT_ETH_USD);
                    }

                    @Override
                    public void onSuccess(Response response, String result) {
                        String ethusd = DEFAULT_ETH_USD;
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            String status = jsonObject.optString("status");
                            if (!TextUtils.isEmpty(status) && status.equals("1")) {
                                JSONObject resultObj = jsonObject.optJSONObject("result");
                                ethusd = resultObj.optString("ethusd");
                            }
                        } catch (JSONException e) {
                        }
                        handleCallback(ethusd);
                    }
                });
            }

            private void handleCallback(String ethusd) {
                BigDecimal ethusdBig = new BigDecimal(ethusd);
                BigDecimal ethToYuanRateBig = ethusdBig.multiply(new BigDecimal(DEFAULT_USD_YUAN));
                priceInfo.ethToYuanRate = ethToYuanRateBig.toString();
                priceInfo.timestamp = System.currentTimeMillis();

                if (callback != null) {
                    callback.onPriceInfo(priceInfo);
                }

                String priceJson = Util.ObjToJson(priceInfo);
                PreferenceManager.getInstance().putString(KEY_PRICE_INFO, priceJson);
            }

        });

    }

}
