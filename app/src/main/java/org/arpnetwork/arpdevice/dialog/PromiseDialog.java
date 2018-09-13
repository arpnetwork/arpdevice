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

package org.arpnetwork.arpdevice.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.contracts.ARPBank;
import org.web3j.utils.Convert;

import java.math.BigInteger;

public class PromiseDialog {
    public interface PromiseListener {
        void onError();
        void onExchange(BigInteger unexchanged);
        void onIgnore();
    }

    public static void show(Context context, int messageId, String ignoreText, final PromiseListener listener) {
        BigInteger unexchanged = null;
        try {
            unexchanged = ARPBank.getUnexchange();
        } catch (Exception e) {
            new AlertDialog.Builder(context)
                    .setMessage(R.string.network_error)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            listener.onError();
                        }
                    })
                    .setCancelable(false)
                    .show();
            return;
        }
        if (unexchanged.compareTo(BigInteger.ZERO) > 0) {

            String message = String.format(context.getString(messageId),
                    Convert.fromWei(unexchanged.toString(), Convert.Unit.ETHER).floatValue());
            final MessageDialog.Builder builder = new MessageDialog.Builder(context);
            final BigInteger finalUnexchanged = unexchanged;
            builder.setTitle(context.getString(R.string.exchange_unchanged_promise_found))
                    .setMessage(message)
                    .setPositiveButton(context.getString(R.string.go_exchange), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            listener.onExchange(finalUnexchanged);
                        }
                    })
                    .setNegativeButton(ignoreText, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            listener.onIgnore();
                        }
                    })
                    .create()
                    .show();
        } else {
            listener.onIgnore();
        }
    }
}
