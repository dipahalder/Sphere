// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.spherelib;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by rangelo on 7/29/15.
 */
public class PhotoSphereConstructor {

    private CameraView mCameraView;
    private int mWidth;
    private int mHeight;
    private int mNumOfPicturesDrawn = 0;

    private Bitmap mBitmap;
    private Canvas mCanvas;

    private String mDestinationFile;
    private File mFile;
    private boolean mIsConstructionDone;

    // manages the drawing processes in at most two threads
    private ExecutorService mExecutorService = Executors.newFixedThreadPool(2);

    private boolean mIsFileSaved;

    private static PhotoSphereConstructor sInstance;

    public static PhotoSphereConstructor getInstance() {
        return sInstance;
    }

    public static PhotoSphereConstructor getInstance(CameraView cameraView, int height) {
        try {
            sInstance = new PhotoSphereConstructor(cameraView, height);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sInstance;
    }


    private PhotoSphereConstructor(CameraView cameraView, int height) {

        mCameraView = cameraView;

        mHeight = height;
        mWidth = 2 * height;

        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void drawPicture() {
        final Runnable mDrawPictureRunnable = new Runnable() {
            @Override
            public void run() {
                mNumOfPicturesDrawn++;
                drawPictureProcess(mCameraView.getPictures().get(mNumOfPicturesDrawn - 1));
                if (mNumOfPicturesDrawn == mCameraView.getPictures().size()) {
                    if (mDestinationFile != null) {
                        savePictureToFile();
                    }
                    mIsConstructionDone = true;
                }
            }
        };

        mExecutorService.execute(mDrawPictureRunnable);
    }

    public void drawPictureProcess(CameraView.Picture picture) {

        Rect rect = new Rect(picture.getBitmap().getWidth() / 6, picture.getBitmap().getWidth()
                * 5 / 6, picture.getBitmap().getHeight() / 6, picture.getBitmap().getHeight()
                - picture.getBitmap().getWidth() / 6);


        int jumpSize = 15;
        boolean moveFast = false;
        int count = 0;

        boolean gotInside;

        Paint dstOverPaint = new Paint();
        dstOverPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));

        Paint srcOverPaint = new Paint();
        srcOverPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));

        Paint helperPaint = new Paint();
        helperPaint.setARGB(50, 255, 0, 0);

        int color;
        int alpha;


        for (int i = 0; i < mWidth; i++) {
            for (int j = 0; j < mHeight - 1; j++) {
                float xAngle = (float) (2 * Math.PI * (float) i / mWidth);
                float yAngle = (float) (Math.PI * (((float) j / mHeight) - 1.0f / 2));
                float[] pointInSphere = new float[]{
                        (float) Math.sin(yAngle),
                        (float) (Math.cos(xAngle) * Math.cos(yAngle)),
                        (float) (Math.sin(xAngle) * Math.cos(yAngle))
                };


                float[] rotated = MatrixUtils.multiply(
                        pointInSphere,
                        MatrixUtils.transpose(picture.getRotationMatrix()));

                gotInside = rotated[2] > 0;

                if (gotInside) {

                    float[] projected = project(rotated);


                    projected[0] *= picture.getBitmap().getWidth()
                            / picture.getAbstractWidth();
                    projected[0] += picture.getBitmap().getWidth() / 2;
                    projected[1] *= picture.getBitmap().getHeight()
                            / picture.getAbstractHeight();
                    projected[1] += picture.getBitmap().getHeight() / 2;

                    gotInside = (projected[0] >= 0
                            && projected[0] < picture.getBitmap().getWidth() - 1
                            && projected[1] >= 0
                            && projected[1] < picture.getBitmap().getHeight() - 1);

                    if (gotInside) {
                        color = picture.getBitmap().getPixel(
                                (int) projected[0],
                                (int) projected[1]);

                        dstOverPaint.setColor(color);
                        mCanvas.drawPoint(i, j, dstOverPaint);

                        alpha = Math.max(
                                0,
                                255
                                        - (255 * rect.distanceSquaredTo(
                                        (int) projected[0],
                                        (int) projected[1])) / (picture.getBitmap().getWidth() / 6)
                                        / (picture.getBitmap().getWidth() / 6));


                        srcOverPaint.setColor(color);
                        srcOverPaint.setAlpha(alpha);
                        mCanvas.drawPoint(i, j, srcOverPaint);
                    }


                }


                if ((!moveFast) && (!gotInside)) {
                    count++;
                }

                if (count > jumpSize + 1) {
                    count = 0;
                    moveFast = true;
                }

                if (moveFast && gotInside) {
                    moveFast = false;
                    j = Math.max(j - jumpSize, 0);
                }

                if (moveFast) {
                    j = j + jumpSize;
                }


            }
        }
    }

    public void savePictureToFile() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] data = stream.toByteArray();
        try {
            mFile = getNewFile(mDestinationFile);
            FileOutputStream fos = new FileOutputStream(mFile);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        mIsFileSaved = true;
    }

    public float requestProgress() {
        return (float) mNumOfPicturesDrawn / mCameraView.getPictures().size();
    }


    private float[] project(float[] point) {
        return new float[]{point[0] / point[2], point[1] / point[2]};
    }

    private File getNewFile(String fileName) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), mCameraView.getContext().getPackageName());

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("info_", "Failed to create directory");
                return null;
            }
        }

        return new File(mediaStorageDir.getPath() + File.separator +
                "IMG_" + fileName + ".jpg");
    }

    private class Rect {
        private int xmin;
        private int xmax;
        private int ymin;
        private int ymax;

        Rect(int l, int r, int b, int t) {
            xmin = l;
            xmax = r;
            ymin = b;
            ymax = t;
        }

        public int distanceSquaredTo(int x, int y) {
            int dx = 0, dy = 0;
            if (x < xmin) {
                dx = x - xmin;
            } else if (x > xmax) {
                dx = x - xmax;
            }

            if (y < ymin) {
                dy = y - ymin;
            } else if (y > ymax) {
                dy = y - ymax;
            }

            return dx * dx + dy * dy;
        }

    }

    public void setDestinationFile(String destinationFile) {
        mDestinationFile = destinationFile;
    }

    public boolean isConstructionDone() {
        return mIsConstructionDone;
    }

    public boolean isFileSaved() {
        return mIsFileSaved;
    }

    public File getFile() {
        return mFile;
    }
}
