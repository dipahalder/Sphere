// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.fbu.photosphere.spherelib.SphereView;

public class ViewerActivity extends FragmentActivity {
    private static final String TAG = ViewerActivity.class.getSimpleName();
    public static final String IMAGE_URL = "IMAGE_URL";
    private SphereView mPhotosphereView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewer_activity);
        Bundle extras = getIntent().getExtras();
        mPhotosphereView = (SphereView) findViewById(R.id.photosphere_view);
        if (extras == null) {
            //show error message and finish activity
            Toast toast = Toast
                    .makeText(this, getString(R.string.photosphere_loading_error),
                            Toast.LENGTH_SHORT);
            toast.show();
            finish();
        } else {
            String imageUrl = extras.getString(IMAGE_URL);
            mPhotosphereView.setMode(SphereView.SphereViewMode.COMPASS);
            mPhotosphereView.setIsZoomAllowed(true);
            mPhotosphereView.setIsDoubleClickSwitchAllowed(true);
            mPhotosphereView.setKeepScreenOn(true);
            mPhotosphereView.setImageUrl(imageUrl);
            final ImageView modeButton = (ImageView) findViewById(R.id.modes);
            modeButton.bringToFront();
            modeButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    switch (mPhotosphereView.getMode()) {
                        case COMPASS:
                            modeButton.setImageResource(R.drawable.compass_mode);
                            mPhotosphereView.setMode(SphereView.SphereViewMode.TOUCH);
                            break;
                        case TOUCH:
                            modeButton.setImageResource(R.drawable.touch_mode);
                            mPhotosphereView.setMode(SphereView.SphereViewMode.COMPASS);
                            break;
                    }
                }
            });
        }
    }
}
