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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import org.arpnetwork.arpdevice.ui.widget.EmptyView;

public abstract class BaseFragment extends Fragment {
    private EmptyView mEmptyView;

    private ProgressDialog mProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        addEmptyView();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    protected BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

    protected void hideNavIcon() {
        Toolbar toolbar = getBaseActivity().getToolbar();
        if (toolbar != null) {
            toolbar.setNavigationIcon(null);
        }
    }

    protected void setTitle(CharSequence title) {
        if (getActivity() != null) getActivity().setTitle(title);
    }

    protected void setTitle(int resId) {
        setTitle(getString(resId));
    }

    protected final View findViewById(int viewId) {
        return getView().findViewById(viewId);
    }

    protected void showProgressDialog(String msg) {
        showProgressDialog(msg, true);
    }

    protected void showProgressDialog(String msg, boolean cancel) {
        mProgressDialog = ProgressDialog.show(getActivity(), null, msg);
        mProgressDialog.setCanceledOnTouchOutside(cancel);
        mProgressDialog.setCancelable(cancel);
    }

    protected void hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    protected void startActivity(Class<?> cls) {
        startActivity(cls, null);
    }

    protected void startActivity(Class<?> cls, Bundle bundle) {
        Intent intent = new Intent(getActivity(), cls);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        startActivity(intent);
    }

    protected void startActivityForResult(Class<?> cls, int requestCode) {
        startActivityForResult(cls, requestCode, null);
    }

    protected void startActivityForResult(Class<?> cls, int requestCode, Bundle bundle) {
        Intent intent = new Intent(getActivity(), cls);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        startActivityForResult(intent, requestCode);
    }

    protected void finish() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            getActivity().finish();
        }
    }

    protected void runOnUiThread(Runnable action) {
        getActivity().runOnUiThread(action);
    }

    protected void hideSoftInput(View view) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void addEmptyView() {
        if (getContentView() != null) {
            if (mEmptyView == null) {
                mEmptyView = new EmptyView(getActivity());
                mEmptyView.setOnRefreshListener(mOnRefreshListener);

                ((ViewGroup) getContentView().getParent()).addView(mEmptyView);
            } else {
                if (mEmptyView.getState() != EmptyView.STATE_INIT) {
                    mEmptyView = null;
                    addEmptyView();
                }
            }
        }
    }

    protected EmptyView getEmptyView() {
        return mEmptyView;
    }

    protected View getContentView() {
        return null;
    }

    protected void showContentView() {
        if (getContentView() != null) {
            getContentView().setVisibility(View.VISIBLE);
        }
        if (getEmptyView() != null) {
            getEmptyView().setVisibility(View.GONE);
        }
    }

    protected void showEmptyView(int state) {
        showEmptyView(state, 0);
    }

    protected void showEmptyView(int state, int tips) {
        if (getContentView() != null) {
            getContentView().setVisibility(View.GONE);
        }
        if (getEmptyView() != null) {
            getEmptyView().setVisibility(View.VISIBLE);
            getEmptyView().setEmptyTips(tips);
            getEmptyView().updateViews(state);
        }
    }

    protected void loadData() {
    }

    private EmptyView.OnRefreshListener mOnRefreshListener = new EmptyView.OnRefreshListener() {
        @Override
        public void onRefresh() {
            if (getEmptyView() != null) {
                getEmptyView().updateViews(EmptyView.STATE_INIT);
                loadData();
            }
        }
    };
}
