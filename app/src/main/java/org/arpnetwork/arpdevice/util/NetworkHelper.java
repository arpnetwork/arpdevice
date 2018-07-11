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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public class NetworkHelper {
    private static final String TAG = NetworkHelper.class.getSimpleName();

    private static NetworkHelper sInstance = null;

    private Context mContext;
    private NetworkMonitor mNetworkMonitor;
    private Set<NetworkChangeListener> mNetworkListeners;

    private Handler mHandler;
    private ConnectivityManager mCM;

    public interface NetworkChangeListener {
        void onNetworkChange(NetworkInfo info);
    }

    public static void init(Context context) {
        sInstance = new NetworkHelper(context);
    }

    public static void fini() {
        if (sInstance != null) {
            sInstance.release();
            sInstance = null;
        }
    }

    public static NetworkHelper getInstance() {
        return sInstance;
    }

    private NetworkHelper(Context context) {
        mContext = context;
        mNetworkListeners = new HashSet<NetworkChangeListener>();
        mHandler = new Handler();

        mNetworkMonitor = new NetworkMonitor(this);
        mContext.registerReceiver(mNetworkMonitor, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mCM = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private void release() {
        if (!mNetworkListeners.isEmpty()) {
            Log.w(TAG, "listeners is not empty.");
        }
        mNetworkListeners.clear();
        mContext.unregisterReceiver(mNetworkMonitor);
    }

    public void registerNetworkListener(NetworkChangeListener listener) {
        assert (listener != null);

        mNetworkListeners.add(listener);
    }

    public void unregisterNetworkListener(NetworkChangeListener listener) {
        assert (listener != null);

        mNetworkListeners.remove(listener);
    }

    public boolean isNetworkAvailable() {
        return mCM.getActiveNetworkInfo() != null;
    }

    public boolean isMobileNetwork() {
        return getNetworkType() == ConnectivityManager.TYPE_MOBILE;
    }

    public boolean isWifiNetwork() {
        return getNetworkType() == ConnectivityManager.TYPE_WIFI;
    }

    public int getNetworkType() {
        return getNetworkType(mCM.getActiveNetworkInfo());
    }

    public static int getNetworkType(NetworkInfo networkInfo) {
        return networkInfo != null ? networkInfo.getType() : -1;
    }

    private void onReceiveNetworkBroadcast() {
        final NetworkInfo activeInfo = mCM.getActiveNetworkInfo();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (NetworkChangeListener listener : mNetworkListeners) {
                    listener.onNetworkChange(activeInfo);
                }
            }
        });
    }

    private static class NetworkMonitor extends BroadcastReceiver {
        private WeakReference<NetworkHelper> mContext;

        public NetworkMonitor(NetworkHelper context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                NetworkHelper helper = mContext.get();
                if (helper != null) {
                    helper.onReceiveNetworkBroadcast();
                }
            }
        }
    }
}
