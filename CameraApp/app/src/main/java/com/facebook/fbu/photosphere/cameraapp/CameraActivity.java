// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.cameraapp;

import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class CameraActivity extends ActionBarActivity {
    private static final String TAG = CameraActivity.class.getSimpleName();
    private Camera mCamera;
    private CameraView mCameraView;

    // We overwrite pictures taken in previous uses of the app
    private int mPictureIndex = 0;

    private final Handler mHandler = new Handler();

    private final Runnable mPeriodicPicture = new Runnable() {
        public void run() {
            mCamera.takePicture(null, null, mPicture);
            Toast.makeText(CameraActivity.this, "New picture taken!", Toast.LENGTH_SHORT).show();
            mHandler.postDelayed(mPeriodicPicture, 3000);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        mHandler.postDelayed(mPeriodicPicture, 3000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHandler.removeCallbacks(mPeriodicPicture);
    }

    private Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            Log.e(TAG, "Failed to open camera");
        }
        return camera; // returns null if camera is unavailable
    }

    private File getNewFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), getString(R.string.app_name));

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Failed to create directory");
                return null;
            }
        }

        return new File(mediaStorageDir.getPath() + File.separator +
                "IMG_" + Integer.toString(mPictureIndex) + ".jpg");
    }

    private PictureCallback mPicture = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = getNewFile();
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                mPictureIndex++;
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mCamera = getCameraInstance();
        mCamera.setDisplayOrientation(90); // sets orientation to portrait

        mCameraView = new CameraView(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_view);
        preview.addView(mCameraView);
    }
}
