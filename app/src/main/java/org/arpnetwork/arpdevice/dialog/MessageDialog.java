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

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.arpnetwork.arpdevice.R;

public class MessageDialog extends Dialog {

    public MessageDialog(Context context) {
        super(context);
    }

    public MessageDialog(Context context, int theme) {
        super(context, theme);
    }

    public static class Builder {
        private Context mContext;
        private String mTitle;
        private String mMessage;
        private CharSequence mPositiveButtonText;
        private OnClickListener mPositiveButtonListener;
        private CharSequence mNegativeButtonText;
        private OnClickListener mNegativeButtonListener;

        public Builder(Context context) {
            this.mContext = context;
        }

        public MessageDialog.Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public MessageDialog.Builder setMessage(String message) {
            mMessage = message;
            return this;
        }

        public MessageDialog.Builder setPositiveButton(CharSequence text, OnClickListener listener) {
            mPositiveButtonText = text;
            mPositiveButtonListener = listener;
            return this;
        }

        public MessageDialog.Builder setNegativeButton(CharSequence text, OnClickListener listener) {
            mNegativeButtonText = text;
            mNegativeButtonListener = listener;
            return this;
        }

        public MessageDialog create() {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            final MessageDialog dialog = new MessageDialog(mContext, R.style.Dialog);
            View layout = inflater.inflate(R.layout.dialog_message, null);
            dialog.addContentView(layout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            if (mTitle != null) {
                ((TextView) layout.findViewById(R.id.tv_title)).setText(mTitle);
            }
            if (mMessage != null) {
                ((TextView) layout.findViewById(R.id.tv_message)).setText(mMessage);
            }

            if (mNegativeButtonText != null) {
                ((TextView) layout.findViewById(R.id.btn_cancel)).setText(mNegativeButtonText);
                layout.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        dialog.dismiss();
                        if (mNegativeButtonListener != null) {
                            mNegativeButtonListener.onClick(dialog, 0);
                        }
                    }
                });
            } else {
                layout.findViewById(R.id.btn_cancel).setVisibility(View.GONE);
            }

            if (mPositiveButtonText != null) {
                ((Button) layout.findViewById(R.id.btn_ok)).setText(mPositiveButtonText);
                layout.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        dialog.dismiss();
                        if (mPositiveButtonListener != null) {
                            mPositiveButtonListener.onClick(dialog, 1);
                        }
                    }
                });
            }
            return dialog;
        }
    }
}
