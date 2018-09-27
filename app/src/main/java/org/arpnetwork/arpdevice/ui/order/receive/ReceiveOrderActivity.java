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

package org.arpnetwork.arpdevice.ui.order.receive;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.WindowManager;

import org.arpnetwork.arpdevice.ui.base.BaseActivity;
import org.arpnetwork.arpdevice.util.UIHelper;

public class ReceiveOrderActivity extends BaseActivity {
    private float mEventY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);

        setContentFragment(ReceiveOrderFragment.class);
    }

    @Override
    protected void onDestroy() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mEventY = ev.getY();
                break;
            case MotionEvent.ACTION_UP:
                if (mEventY <= UIHelper.getStatusbarHeight(getApplicationContext()) && ev.getY() > mEventY + 10) {
                    finish();
                    return true;
                }
                mEventY = 0;
                break;
        }
        return super.dispatchTouchEvent(ev);
    }
}
