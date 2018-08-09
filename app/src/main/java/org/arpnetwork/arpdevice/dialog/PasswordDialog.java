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
import android.widget.EditText;

import org.arpnetwork.arpdevice.R;

public class PasswordDialog extends Dialog {

    public PasswordDialog(Context context) {
        super(context);
    }

    public PasswordDialog(Context context, int theme) {
        super(context, theme);
    }

    public static class Builder {
        private Context mContext;
        private EditText mPasswordView;
        private OnClickListener mButtonListener;

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder setOnClickListener(OnClickListener listener) {
            mButtonListener = listener;
            return this;
        }

        public String getPassword() {
            if (mPasswordView != null) {
                return mPasswordView.getText().toString().trim();
            }
            return "";
        }

        public PasswordDialog create() {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            final PasswordDialog dialog = new PasswordDialog(mContext, R.style.Dialog);
            View layout = inflater.inflate(R.layout.dialog_password, null);
            dialog.addContentView(layout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            mPasswordView = layout.findViewById(R.id.et_password);
            layout.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mButtonListener != null) {
                        mButtonListener.onClick(dialog, 0);
                    }
                }
            });
            return dialog;
        }
    }
}
