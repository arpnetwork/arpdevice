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

package org.arpnetwork.arpdevice.ui.wallet;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.my.MyActivity;
import org.arpnetwork.arpdevice.util.UIHelper;
import org.web3j.crypto.WalletUtils;

public class WalletImporterFragment extends BaseFragment {
    private EditText mEditPrivateKey;
    private EditText mEditWalletName;
    private EditText mEditPassword;
    private EditText mEditConfirmedPassword;
    private Button mBtnImport;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.import_wallet);
        hideNavIcon();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wallet_importer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews();
    }

    private void initViews() {
        mEditPrivateKey = (EditText) findViewById(R.id.et_private_key);
        mEditWalletName = (EditText) findViewById(R.id.et_wallet_name);
        mEditPassword = (EditText) findViewById(R.id.et_password);
        mEditConfirmedPassword = (EditText) findViewById(R.id.et_confirm_password);
        mBtnImport = (Button) findViewById(R.id.btn_import);
        mBtnImport.setOnClickListener(mOnClickImportListener);
    }

    private boolean checkInputs(String privateKey, String walletName, String password, String confirmedPassword) {
        if (TextUtils.isEmpty(privateKey) || TextUtils.isEmpty(walletName)
                || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmedPassword)) {
            UIHelper.showToast(getContext(), R.string.complete_info);
            return false;
        }

        if (!password.equals(confirmedPassword)) {
            UIHelper.showToast(getContext(), R.string.inconsistent_password);
            return false;
        }

        if (password.length() < 8) {
            UIHelper.showToast(getContext(), R.string.use_more_characters_password);
            return false;
        }

        if (!WalletUtils.isValidPrivateKey(privateKey)) {
            UIHelper.showToast(getContext(), R.string.invalid_private_key);
            return false;
        }

        return true;
    }

    private void importWallet() {
        String privateKey = mEditPrivateKey.getText().toString().trim();
        String walletName = mEditWalletName.getText().toString().trim();
        String password = mEditPassword.getText().toString().trim();
        String confirmedPassword = mEditConfirmedPassword.getText().toString().trim();

        if (checkInputs(privateKey, walletName, password, confirmedPassword)) {
            showProgressDialog(getString(R.string.importing), false);
            Wallet.importWallet(getContext(), walletName, privateKey, password, new Wallet.Callback() {
                @Override
                public void onCompleted(final boolean success) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideProgressDialog();
                            int resId = success ? R.string.import_success : R.string.import_failed;
                            UIHelper.showToast(getContext(), resId);
                            if (success) {
                                startActivity(MyActivity.class);
                                finish();
                            }
                        }
                    });
                }
            });
        }
    }

    private View.OnClickListener mOnClickImportListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            importWallet();
        }
    };
}
