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

package org.arpnetwork.arpdevice.ui.base;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.util.UIHelper;

public class BaseActivity extends AppCompatActivity {
    private Toolbar mToolbar;
    private TextView mTitleView;
    private OnBackListener mOnBackListener;

    private long mExitTime = 0;

    public interface OnBackListener {
        boolean onBacked();
    }

    public void setOnBackListener(OnBackListener listener) {
        mOnBackListener = listener;
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    public void showToolbar() {
        mToolbar.setVisibility(View.VISIBLE);
    }

    public void hideToolbar() {
        mToolbar.setVisibility(View.GONE);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle("");

        mTitleView.setText(title);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                } else {
                    boolean pressed = false;
                    if (mOnBackListener != null) {
                        pressed = mOnBackListener.onBacked();
                    }
                    if (!pressed) {
                        finish();
                    }
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        boolean pressed = onBack();
        if (!pressed) {
            if (onExitApp()) {
                if ((System.currentTimeMillis() - mExitTime) > 2000) {
                    UIHelper.showToast(getApplicationContext(), R.string.exit_app, Toast.LENGTH_SHORT);
                    mExitTime = System.currentTimeMillis();
                } else {
                    exit();
                }
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView();
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.content_frame);
        if (fragment != null) {
            fragment = Fragment.instantiate(this, fragment.getClass().getName(), intent.getExtras());
            fm.beginTransaction().replace(R.id.content_frame, fragment).commit();
        }
    }

    protected void setContentView() {
        setContentView(R.layout.content_frame);
    }

    protected void initViews() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTitleView = (TextView) findViewById(R.id.tv_title);
    }

    protected void setToolbar(Toolbar toolbar) {
        mToolbar = toolbar;
    }

    protected void setContentFragment(Class<? extends BaseFragment> fragmentClass) {
        Bundle arguments = null;
        if (getIntent() != null) {
            arguments = getIntent().getExtras();
        }
        setContentFragment(fragmentClass, arguments);
    }

    protected void setContentFragment(Class<? extends BaseFragment> fragmentClass, Bundle arguments) {
        Fragment fragment = Fragment.instantiate(this, fragmentClass.getName(), arguments);
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.content_frame, fragment);
        t.commit();
    }

    protected boolean onBack() {
        boolean pressed = false;
        if (mOnBackListener != null) {
            pressed = mOnBackListener.onBacked();
        }
        return pressed;
    }

    protected boolean onExitApp() {
        return false;
    }

    private void exit() {
        getApplication().onTerminate();
    }
}
