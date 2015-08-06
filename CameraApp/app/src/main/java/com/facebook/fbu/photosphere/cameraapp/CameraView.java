// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.cameraapp;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraView";
    private SurfaceHolder mCameraHolder;
    private Camera mCamera;

    public CameraView(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        mCameraHolder = getHolder();
        mCameraHolder.addCallback(this);

        mCameraHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error starting camera preview");
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mCameraHolder.getSurface() == null) {
            return;
        }

        try {
            mCamera.stopPreview();
        } catch (Exception e) {

        }

        try {
            mCamera.setPreviewDisplay(mCameraHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error reseting camera preview");
        }
    }
}
