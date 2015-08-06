// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.facebook.fbu.photosphere.spherelib.CameraView;

/**
 * Camera activity hosts spherical camera as well as potential to upload existing photo.
 */
public class CameraActivity extends Activity {

    public static final String TAG = CameraActivity.class.getSimpleName();
    private CameraView mCameraView;
    private FrameLayout mFrameLayout;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);
        // create new instance of the camera view
        mCameraView = new CameraView(CameraActivity.this);
        mFrameLayout = (FrameLayout) findViewById(R.id.camera_frame_layout);
        //Camera Directions
        final FrameLayout directions = (FrameLayout) findViewById(R.id.directions);
        TextView topText = (TextView) findViewById(R.id.top_text);
        TextView bottomText = (TextView) findViewById(R.id.bottom_text);
        FontUtils.applyOpenSans(this, topText);
        FontUtils.applyOpenSans(this, bottomText);
        directions.setVisibility(View.VISIBLE);
        mFrameLayout.setVisibility(View.INVISIBLE);
        mFrameLayout.addView(mCameraView);
        //mDirections.bringToFront();
        final ViewSwitcher viewSwitcher = (ViewSwitcher) findViewById(R.id.lower_panel_switcher);
        viewSwitcher.bringToFront();
        Button captureButton = (Button) findViewById(R.id.capture_photo);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewSwitcher.showNext();
                directions.setVisibility(View.INVISIBLE);
                mFrameLayout.setVisibility(View.VISIBLE);
                mFrameLayout.bringChildToFront(viewSwitcher);
            }
        });
        ImageButton uploadButton = (ImageButton) findViewById(R.id.upload_photo);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainActivityIntent = new Intent(
                        CameraActivity.this,
                        UploadPhotoActivity.class);
                startActivity(mainActivityIntent);
            }
        });
        ImageButton cancelButton = (ImageButton) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(CameraActivity.this)
                        .setTitle(R.string.discard_title)
                        .setMessage(R.string.discard_message)
                        .setPositiveButton(
                                android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        mFrameLayout.removeView(mCameraView);
                                        finish();
                                    }
                                })
                        .setNegativeButton(
                                android.R.string.no,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // cancel
                                    }
                                })
                        .show();
            }
        });
        ImageButton backButton = (ImageButton) findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraView.popPicture();
            }
        });

        // finish photosphere capture and launch upload activity
        ImageButton doneButton = (ImageButton) findViewById(R.id.done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraView.startConstruction();
                Intent uploadPhotoIntent = new Intent(
                        CameraActivity.this,
                        UploadPhotoActivity.class);
                uploadPhotoIntent.putExtra(TAG, true);
                startActivity(uploadPhotoIntent);
                finish();
            }
        });
    }

}
