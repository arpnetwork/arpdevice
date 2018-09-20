package org.arpnetwork.arpdevice.ui.widget;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.arpnetwork.arpdevice.R;

public class EmptyView extends RelativeLayout {
    private TextView mTip;
    private ImageView mErrorImage;
    private Button mErrorAction;

    public static final int STATE_INIT = 0;
    public static final int STATE_NO_NETWORK = 1;
    public static final int STATE_FAIL = 2;
    public static final int STATE_EMPTY = 3;

    private int mState = STATE_INIT;

    private int mEmptyBtnStr, mEmptyStr;

    private OnRefreshListener mOnRefreshListener;
    private OnClickListener mOnClickListener;

    public EmptyView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmptyView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init();
    }

    public EmptyView(Context context) {
        super(context);

        init();
    }

    public void updateViews(int state) {
        mState = state;
        switch (state) {
            case STATE_NO_NETWORK:
                mTip.setText(R.string.no_avaliable_network);

                mErrorImage.clearAnimation();

                mErrorImage.setVisibility(View.VISIBLE);
                mErrorImage.setImageResource(R.mipmap.empty_view_refresh_icon);
                mErrorAction.setVisibility(View.GONE);
                break;
            case STATE_INIT:
                mTip.setText(R.string.loading_data);

                mErrorImage.setVisibility(View.VISIBLE);
                mErrorAction.setVisibility(View.GONE);
                break;
            case STATE_FAIL:
                mTip.setText(R.string.load_data_fail);

                mErrorImage.clearAnimation();

                mErrorImage.setImageResource(R.mipmap.empty_view_refresh_icon);
                mErrorImage.setVisibility(View.VISIBLE);
                mErrorAction.setVisibility(View.GONE);
                break;
            case STATE_EMPTY:
                mErrorImage.clearAnimation();

                if (mEmptyBtnStr == 0 && mEmptyStr == 0) {
                    mTip.setText(R.string.load_data_empty);

                    mErrorImage.setImageResource(R.mipmap.empty_view_refresh_icon);
                    mErrorImage.setVisibility(View.VISIBLE);
                    mErrorAction.setVisibility(View.GONE);
                } else if (mEmptyBtnStr == 0 && mEmptyStr != 0) {
                    mTip.setText(mEmptyStr);

                    mErrorImage.setImageResource(R.mipmap.empty_view_refresh_icon);
                    mErrorImage.setVisibility(View.VISIBLE);
                    mErrorAction.setVisibility(View.GONE);
                } else {
                    mTip.setText(mEmptyStr);
                    mErrorAction.setText(mEmptyBtnStr);

                    mErrorImage.setVisibility(View.GONE);
                    mErrorAction.setVisibility(View.VISIBLE);
                }
                break;

            default:
                break;
        }
    }

    public int getState() {
        return mState;
    }

    public void configEmptyState(int buttonText, int tips) {
        mEmptyBtnStr = buttonText;
        mEmptyStr = tips;
    }

    public void setEmptyTips(int tips) {
        mEmptyStr = tips;
    }

    protected void init() {
        inflate(getContext(), R.layout.empty_view, this);
        mTip = (TextView) findViewById(R.id.tv_tip);
        mErrorImage = (ImageView) findViewById(R.id.iv_error);
        mErrorAction = (Button) findViewById(R.id.iv_error_action);
        mErrorAction.setOnClickListener(mActionLister);
        findViewById(R.id.errorview_container).setOnClickListener(mInterClickListener);

        updateViews(STATE_INIT);
    }

    private OnClickListener mInterClickListener = new OnClickListener() {

        public void onClick(View v) {
            switch (mState) {
                case STATE_NO_NETWORK:
                    v.getContext().startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                    break;
                case STATE_FAIL:
                    if (mOnRefreshListener != null) {
                        mOnRefreshListener.onRefresh();
                    }
                    break;

                default:
                    break;
            }
        }

    };

    OnClickListener mActionLister = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mOnClickListener != null) {
                mOnClickListener.onClick(v);
            }
        }
    };

    public void setOnRefreshListener(OnRefreshListener listener) {
        mOnRefreshListener = listener;
    }

    public interface OnRefreshListener {
        public void onRefresh();
    }

    public void setOnClickListener(OnClickListener listener) {
        mOnClickListener = listener;
    }
}