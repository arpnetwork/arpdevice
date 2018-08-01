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

package org.arpnetwork.arpdevice.ui.order;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import org.arpnetwork.arpdevice.R;
import org.arpnetwork.arpdevice.ui.base.BaseFragment;

public class OrderDetailsFragment extends BaseFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.order_details);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_details, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initViews() {
        View footerView = new View(getContext());
        footerView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.content_padding)));
        footerView.setBackgroundResource(R.color.window_background_light_gray);

        OrderDetailsHeader headerView = new OrderDetailsHeader(getContext());
        headerView.setVisibility(View.GONE);

        OrderDetailsAdapter adapter = new OrderDetailsAdapter(getContext());
        ListView listView = (ListView) findViewById(R.id.listview);
        listView.addHeaderView(headerView);
        listView.addFooterView(footerView);
        listView.setAdapter(adapter);
    }
}
