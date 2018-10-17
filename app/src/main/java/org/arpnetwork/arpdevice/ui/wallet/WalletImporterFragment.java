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

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.constant.Constant;
import org.arpnetwork.arpdevice.data.Promise;
import org.arpnetwork.arpdevice.database.EarningRecord;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;
import org.arpnetwork.arpdevice.ui.home.HomeActivity;
import org.arpnetwork.arpdevice.util.SignUtil;
import org.arpnetwork.arpdevice.util.UIHelper;
import org.web3j.crypto.WalletUtils;

public class WalletImporterFragment extends BaseFragment {
    private EditText mEditPrivateKey;
    private EditText mEditPassword;
    private EditText mEditConfirmedPassword;
    private Button mBtnImport;
    private boolean mFromLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.import_wallet);
        hideNavIcon();

        Bundle bundle = getArguments();
        if (bundle != null) {
            mFromLauncher = bundle.getBoolean(Constant.KEY_FROM_LAUNCHER);
        }
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

    public boolean onExitApp() {
        return mFromLauncher;
    }

    private void initViews() {
        mEditPrivateKey = (EditText) findViewById(R.id.et_private_key);
        mEditPassword = (EditText) findViewById(R.id.et_password);
        mEditConfirmedPassword = (EditText) findViewById(R.id.et_confirm_password);

        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (source.equals(" ")) {
                    return "";
                }
                return null;
            }
        };
        mEditPassword.setFilters(new InputFilter[]{filter});
        mEditConfirmedPassword.setFilters(new InputFilter[]{filter});

        mBtnImport = (Button) findViewById(R.id.btn_import);
        mBtnImport.setOnClickListener(mOnClickImportListener);

        Toolbar toolbar = getBaseActivity().getToolbar();
        ImageButton scannerIcon = new ImageButton(toolbar.getContext());
        scannerIcon.setImageResource(R.mipmap.qrcode);
        scannerIcon.setBackgroundColor(Color.TRANSPARENT);

        scannerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();
            }
        });
        Toolbar.LayoutParams lp = new Toolbar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT);
        lp.rightMargin = (int) getResources().getDimension(R.dimen.plain_margin);
        toolbar.addView(scannerIcon, lp);
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.CAMERA)) {
                    new AlertDialog.Builder(getActivity())
                            .setMessage(getString(R.string.required_camera_permission))
                            .setPositiveButton(R.string.ok, null)
                            .create()
                            .show();
                } else {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.CAMERA},
                            Constant.PERMISSION_REQUEST_CODE_CAMERA);
                }
                return ;
            }
        }
        scanPrivateKey();
    }

    private boolean checkInputs(String privateKey, String password, String confirmedPassword) {
        if (TextUtils.isEmpty(privateKey) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmedPassword)) {
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
        String password = mEditPassword.getText().toString();
        String confirmedPassword = mEditConfirmedPassword.getText().toString();

        if (checkInputs(privateKey, password, confirmedPassword)) {
            showProgressDialog(getString(R.string.importing), false);
            Wallet.importWallet(getContext(), privateKey, password, new Wallet.Callback() {
                @Override
                public void onCompleted(final boolean success) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideProgressDialog();
                            int resId = success ? R.string.import_success : R.string.import_failed;
                            UIHelper.showToast(getContext(), resId);
                            if (success) {
                                // clear earning record
                                EarningRecord.clear();
                                Promise.clear();
                                SignUtil.resetSigner();
                                Intent intent = new Intent(getActivity(), HomeActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constant.PERMISSION_REQUEST_CODE_CAMERA:
                scanPrivateKey();
                break;

            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constant.QRCODE_REQUEST && data != null) {
            String result = data.getExtras().getString(Constant.ACTIVITY_RESULT_KEY_PRIVATE);
            mEditPrivateKey.setText(result);
            mEditPassword.requestFocus();
        }
    }

    private void scanPrivateKey() {
        startActivityForResult(QRCodeScannerActivity.class, Constant.QRCODE_REQUEST);
    }
}
