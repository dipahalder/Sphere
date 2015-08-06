// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.sphereviewer;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.facebook.fbu.photosphere.spherelib.CameraView;

/**
 * Created by rangelo on 7/23/15.
 */
public class SampleCameraActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final FrameLayout frameLayout = new FrameLayout(this);

        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(frameLayout);


        final CameraView cameraView = new CameraView(this);

        final ImageView imageView = new ImageView(this);

        imageView.setLayoutParams(new ViewGroup.LayoutParams(800, 400));


        frameLayout.addView(cameraView);
        frameLayout.addView(imageView);


        // this displays the bitmap the final picture is being constructed on an imageView
        // eventually we will have a construction finished callback


        // clicking the image will call this method that saves the bitmap to a file
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.startConstruction();
                cameraView.savePictureToFileWhenDone("my_filename");
            }
        });

    }

}
