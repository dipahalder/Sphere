// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.sphereviewer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.Gallery;

import com.facebook.fbu.photosphere.spherelib.SphereView;

import java.io.IOException;


/**
 * This file is a sample of how to use a SphereView
 */
public class SampleViewActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bitmap b = null;
        try {
            b = BitmapFactory.decodeStream(getAssets().open("picture4.jpg"));
        } catch (IOException ioe) {

        }
        // create a SphereView by giving context and the photosphere bitmap
        SphereView sphereView = new SphereView(this);
        sphereView.setBitmap(b);

        setContentView(R.layout.canvas_test);
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.linear_layout);

        // sets the layout parameters if desired. match_parent is default
        sphereView.setLayoutParams(new Gallery.LayoutParams(1200, 800));

        frameLayout.addView(sphereView);

        // changes between compass mode and manual mode. COMPASS is default
        // PREVIEW mode just switches right away to TOUCH mode, but launches it with a initial
        // rotating velocity and small friction
        sphereView.setMode(SphereView.SphereViewMode.PREVIEW);

        // allows or not two finger zoom. default is true
        sphereView.setIsZoomAllowed(true);

        // allows or not switching between COMPASS and TOUCH mode via double click. default is true
        // we will probably want to implement a fancy interface for switching
        sphereView.setIsDoubleClickSwitchAllowed(true);

        // this prohibits the screen from sleeping while the SphereView is visible
        sphereView.setKeepScreenOn(true);


    }
}
