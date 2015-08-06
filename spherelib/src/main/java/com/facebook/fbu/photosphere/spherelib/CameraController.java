// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.spherelib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;

import java.util.List;

/**
 * A single of this class is created by a CameraView to handle the camera
 */
public class CameraController {

    private static final String TAG = CameraController.class.getSimpleName();

    private Camera mCamera;
    private Context mContext;

    private OrientationManager mOrientationManager;

    private CameraView.Picture mCurrentPicture;
    private CameraView.ReferencePoint mCurrentReferencePoint;
    private CameraView mParentCameraView;

    private PhotoSphereConstructor mPhotoSphereConstructor;

    private boolean mBusy = false;

    public static CameraController getNewInstance(
            Context context,
            OrientationManager orientationManager,
            CameraView parentCameraView,
            PhotoSphereConstructor photoSphereConstructor) {
        CameraController cameraController = null;
        try {
            Camera camera = Camera.open();
            cameraController = new CameraController(context,
                    orientationManager,
                    parentCameraView,
                    photoSphereConstructor,
                    camera);
        } catch (Exception e) {
            Log.e(TAG, "Unable to open camera", e);
        }

        return cameraController;
    }

    private CameraController(Context context,
                             OrientationManager orientationManager,
                             CameraView parentCameraView,
                             PhotoSphereConstructor photoSphereConstructor,
                             Camera camera) {

        mParentCameraView = parentCameraView;
        mContext = context;
        mPhotoSphereConstructor = photoSphereConstructor;
        mCamera = camera;
        List<Camera.Size> sizeList = mCamera.getParameters().getSupportedPictureSizes();
        Point chosenSize = new Point(-1, -1);
        // we choose the camera size to be the minimum size with width at least 1000
        // I point out here that width > height, because the camera uses landscape as reference
        for (Camera.Size size : sizeList) {
            if (size.width > 1000 && (size.width < chosenSize.x || chosenSize.x == -1)) {
                chosenSize.set(size.width, size.height);
            }
        }

        // if no size above 1000 is available we don't change it and go with the default,
        // which is the maximum one
        if (chosenSize.x != -1) {
            Camera.Parameters params = mCamera.getParameters();
            params.setPictureSize(chosenSize.x, chosenSize.y);
            mCamera.setParameters(params);
        }

        mCamera.startPreview();
        mOrientationManager = orientationManager;

    }

    public void close() {
        mCamera.stopPreview();
        mCamera.release();
        try {
            mCamera.stopPreview();
            mCamera.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CameraView.Picture takePicture(
            String fileName,
            CameraView.ReferencePoint referencePoint) {
        if (mBusy) {
            return null;
        }
        mBusy = true;
        mCurrentPicture = mParentCameraView.getNewPicture();
        mCurrentReferencePoint = referencePoint;
        mCurrentPicture.setRotationMatrix(mOrientationManager.getPositionRotMatrix());
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                        Matrix matrix = new Matrix();
                        matrix.postRotate(90);

                        mCurrentPicture.setBitmap(Bitmap.createBitmap(
                                bitmap,
                                0,
                                0,
                                bitmap.getWidth(),
                                bitmap.getHeight(),
                                matrix,
                                false));
                        mCurrentPicture.setVertices(mCamera.getParameters());
                        mCurrentPicture.setReferencePoint(mCurrentReferencePoint);
                        mCurrentPicture.setIsSaved(true);
                        if (mPhotoSphereConstructor != null) {
                            mPhotoSphereConstructor.drawPicture();
                        }

                        System.gc();
                    }
                });

                mBusy = false;
            }
        });

        return mCurrentPicture;
    }

    public Camera.Parameters getCameraParams() {
        return mCamera.getParameters();
    }

}
