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
import android.widget.SeekBar;
import android.widget.TextView;

import org.arpnetwork.arpdevice.R;

public class SeekBarDialog extends Dialog {

    public SeekBarDialog(Context context) {
        super(context);
    }

    public SeekBarDialog(Context context, int theme) {
        super(context, theme);
    }

    public static class Builder {
        private TextView mSeekValueView;

        private Context mContext;
        private String mTitle;
        private String mMessage;
        private int mProgress;
        private String mSeekValueText;
        private CharSequence mButtonText;
        private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener;
        private OnClickListener mButtonListener;

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder setMessage(String message) {
            mMessage = message;
            return this;
        }

        public Builder setSeekValue(int progress, String valueText) {
            mProgress = progress;
            mSeekValueText = valueText;
            if (mSeekValueView != null) {
                mSeekValueView.setText(mSeekValueText);
            }
            return this;
        }

        public Builder setOnSeekBarChangeListener(SeekBar.OnSeekBarChangeListener listener) {
            mOnSeekBarChangeListener = listener;
            return this;
        }

        public Builder setButton(CharSequence text, OnClickListener listener) {
            mButtonText = text;
            mButtonListener = listener;
            return this;
        }

        public SeekBarDialog create() {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            final SeekBarDialog dialog = new SeekBarDialog(mContext, R.style.Dialog);
            View layout = inflater.inflate(R.layout.dialog_seekbar, null);
            dialog.addContentView(layout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            if (mTitle != null) {
                ((TextView) layout.findViewById(R.id.tv_title)).setText(mTitle);
            }
            if (mMessage != null) {
                ((TextView) layout.findViewById(R.id.tv_message)).setText(mMessage);
            }

            mSeekValueView = layout.findViewById(R.id.tv_seek_value);
            if (mSeekValueText != null) {
                mSeekValueView.setText(mSeekValueText);
            }

            SeekBar seekBar = layout.findViewById(R.id.seekbar);
            seekBar.setProgress(mProgress);
            seekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);

            if (mButtonText != null) {
                ((Button) layout.findViewById(R.id.btn_ok)).setText(mButtonText);
                layout.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        dialog.dismiss();
                        if (mButtonListener != null) {
                            mButtonListener.onClick(dialog, 0);
                        }
                    }
                });
            }
            return dialog;
        }
    }
}
