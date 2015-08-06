// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;

/**
 * Tab View with the 3 main fragments of the application interface (map, upload/camera, profile)
 */
public class MainActivity extends FragmentActivity {
    private FragmentTabHost mTabHost;
    public static final String TAB_1_TAG = "Map";
    public static final String TAB_2_TAG = "Camera";
    public static final String TAB_3_TAG = "Profile";
    // initializes the last tab as tab1
    private static String sLastTab = TAB_1_TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);
        mTabHost.getTabWidget().setDividerDrawable(null);

        mTabHost.addTab(
                mTabHost.newTabSpec(TAB_1_TAG)
                        .setIndicator(getTabIndicator(mTabHost.getContext(), R.mipmap.pin_icon)),
                SphereMapFragment.class,
                null);
        mTabHost.addTab(
                mTabHost.newTabSpec(TAB_2_TAG)
                        .setIndicator(getCameraTabIndicator(mTabHost.getContext(), R.mipmap.camera_button)),
                TabFragment.class,
                null);
        mTabHost.addTab(
                mTabHost.newTabSpec(TAB_3_TAG)
                        .setIndicator(getTabIndicator(mTabHost.getContext(), R.mipmap.profile_icon)),
                ProfileFragment.class,
                null);

        // wires up second tab to launch camera activity, on destroy switches to last tab
        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if (tabId == TAB_2_TAG) {
                    //launch
                    Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                    startActivity(intent);
                    mTabHost.setCurrentTabByTag(sLastTab);
                    return;
                } else if (tabId == TAB_1_TAG) {
                    sLastTab = TAB_1_TAG;
                } else {
                    sLastTab = TAB_3_TAG;
                }
            }
        });
    }

    // Sets up tab indicator layout at bottom of screen
    private View getTabIndicator(Context context, int icon) {
        View view = LayoutInflater.from(context).inflate(
                R.layout.tab_layout,
                new LinearLayout(context));
        ImageView iv = (ImageView) view.findViewById(R.id.imageView);
        iv.setImageResource(icon);
        return view;
    }

    private View getCameraTabIndicator(Context context, int icon) {
        View view = LayoutInflater.from(context).inflate(
                R.layout.camera_tab_layout,
                new LinearLayout(context));
        ImageView iv = (ImageView) view.findViewById(R.id.imageView);
        iv.setImageResource(icon);
        return view;
    }
}

