// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.spherelib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Gallery;

import java.util.ArrayList;
import java.util.List;


public class CameraView extends View {

    // these types will be useful later for stitching
    private enum PointType {
        EQUATOR,
        SOUTH_TROPIC,
        NORTH_TROPIC,
        SOUTH_POLE,
        NORTH_POLE
    }

    private enum DeviceAlignment {
        CORRECT,
        LEFT,
        RIGHT,
        CLOSE_TO_POLE
    }

    private static final int DEFAULT_SPHERE_HEIGHT = 1000;
    private static final float ZOOM_FACTOR = 0.35f;
    private static final double MAXIMUM_ALLOWED_DEVICE_ROTATION = Math.PI / 20;

    private final List<Picture> mPictures = new ArrayList<Picture>();
    private final List<ReferencePoint> mReferencePoints = new ArrayList<ReferencePoint>();
    private final List<MergingRectangle> mMergingRectangles = new ArrayList<MergingRectangle>();

    private final Vibrator mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);

    private OrientationManager mOrientationManager;
    private CameraController mCameraController;
    private PhotoSphereConstructor mPhotoSphereConstructor;

    private float mViewDiameter;

    private Paint mPaint = new Paint();
    
    private boolean mIsPaused;

    private long mAlignmentAnimationOldTime;
    private int mRightAlpha;
    private int mLeftAlpha;

    private Drawable mReferencePointDrawable;
    private Drawable mReferencePointFadeDrawable;
    private Drawable mReferenceCircleDrawable;
    private Drawable mPictureFrame;
    private Drawable mTurnLeftDrawable;
    private Drawable mTurnRightDrawable;
    private int mFrameLeft;
    private int mFrameTop;
    private int mFrameRight;
    private int mFrameBottom;

    public CameraView(Context context) {
        super(context);

        mOrientationManager = new OrientationManager(context);
        mCameraController = CameraController.getNewInstance(
                context,
                mOrientationManager,
                this,
                mPhotoSphereConstructor);

        mPaint.setStrokeWidth(getResources().getDimension(R.dimen.stroke_width));

        // sets the default layout params
        this.setLayoutParams(new Gallery.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        // avoids the screen from sleeping while taking a picture
        this.setKeepScreenOn(true);


        // creates 12 equator reference points
        for (int k = 0; k < 12; k++) {
            mReferencePoints.add(new ReferencePoint(new float[]{0,
                    (float) Math.cos(2 * Math.PI * k / 12),
                    (float) Math.sin(2 * Math.PI * k / 12)},
                    PointType.EQUATOR));
        }

        // creates north and south meridian reference points (9 of each)
        for (int k = 0; k < 9; k++) {
            float angle = (float) Math.PI / 4;
            float[] beforeRot = new float[]{(float) Math.sin(angle),
                    (float) (Math.cos(2 * Math.PI * (k / 9.0f)) * Math.cos(angle)),
                    (float) (Math.sin(2 * Math.PI * (k / 9.0f)) * Math.cos(angle))};
            mReferencePoints.add(new ReferencePoint(beforeRot, PointType.NORTH_TROPIC));
        }
        for (int k = 0; k < 9; k++) {
            float angle = (float) -Math.PI / 4;
            float[] beforeRot = new float[]{(float) Math.sin(angle),
                    (float) (Math.cos(2 * Math.PI * (k / 9.0f)) * Math.cos(angle)),
                    (float) (Math.sin(2 * Math.PI * (k / 9.0f)) * Math.cos(angle))};
            mReferencePoints.add(new ReferencePoint(beforeRot, PointType.SOUTH_TROPIC));
        }

        // creates the south and north pole reference points
        mReferencePoints.add(new ReferencePoint(new float[]{1, 0, 0}, PointType.NORTH_POLE));
        mReferencePoints.add(new ReferencePoint(new float[]{-1, 0, 0}, PointType.SOUTH_POLE));

        // creates 12 merging rectangles at the equator
        // these are not useful for now, but will be used to correct picture locations later
        for (int k = 0; k < 12; k++) {
            double angle = Math.PI / 12 + 2 * Math.PI * k / 12;
            mMergingRectangles.add(new MergingRectangle(new float[][]{
                    {1, 0, 0},
                    {0, (float) Math.cos(angle), -(float) Math.sin(angle)},
                    {0, (float) Math.sin(angle), (float) Math.cos(angle)}
            }, 0.13f, 1f));
        }

        mPictureFrame = getResources().getDrawable(R.drawable.picture_frame);
        mReferencePointDrawable = getResources().getDrawable(R.drawable.camera_view_dot);
        mReferencePointFadeDrawable = getResources().getDrawable(R.drawable.camera_view_dot_fade);
        mReferenceCircleDrawable = getResources().getDrawable(R.drawable.reference_circle);

        mTurnLeftDrawable = getResources().getDrawable(R.drawable.rotateleft);
        mTurnRightDrawable = getResources().getDrawable(R.drawable.rotateright);


    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mOrientationManager.start();
        // the camera api automatically restarts the camera

        postOnAnimation(mSetFrame);
        start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        float abstractWidth = (float) Math.tan((mCameraController.getCameraParams().getVerticalViewAngle()
                * Math.PI / 180 / 2));
        float abstractHeight = (float) Math.tan((mCameraController.getCameraParams().getHorizontalViewAngle()
                * Math.PI / 180 / 2));
        mFrameLeft = (int) (getWidth() / 2 - abstractWidth * ZOOM_FACTOR * mViewDiameter);
        mFrameRight = (int) (getWidth() / 2 + abstractWidth * ZOOM_FACTOR * mViewDiameter);
        mFrameBottom = (int) (getHeight() / 2 + abstractHeight * ZOOM_FACTOR * mViewDiameter);
        mFrameTop = (int) (getHeight() / 2 - abstractHeight * ZOOM_FACTOR * mViewDiameter);
        mPictureFrame.setBounds(mFrameLeft, mFrameTop, mFrameRight, mFrameBottom);

        mReferenceCircleDrawable.setBounds(
                (int) (getWidth() / 2 - getResources().getDimension(R.dimen.outer_circle_radius)),
                (int) (getHeight() / 2 - getResources().getDimension(R.dimen.outer_circle_radius)),
                (int) (getWidth() / 2 + getResources().getDimension(R.dimen.outer_circle_radius)),
                (int) (getHeight() / 2 + getResources().getDimension(R.dimen.outer_circle_radius)));


        int rotateIconSize = (int) getResources().getDimension(R.dimen.rotate_icon_size);
        int rotateIconMargin = (int) getResources().getDimension(R.dimen.rotate_icon_margin);

        mTurnLeftDrawable.setBounds(getWidth() - rotateIconSize - rotateIconMargin, rotateIconMargin, getWidth() - rotateIconMargin, rotateIconSize + rotateIconMargin);
        mTurnRightDrawable.setBounds(rotateIconMargin, rotateIconMargin, rotateIconSize + rotateIconMargin, rotateIconSize + rotateIconMargin);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mOrientationManager.stop();
        mCameraController.close();

        System.gc();
    }


    public void pause() {
        mIsPaused = true;
        mOrientationManager.stop();
        mCameraController.close();

        removeCallbacks(mSetFrame);

        System.gc();
    }

    public void start() {
        mIsPaused = false;
        mOrientationManager.start();
        // the camera api automatically restarts the camera

        postOnAnimation(mSetFrame);
    }

    private final Runnable mSetFrame = new Runnable() {
        public void run() {
            if (mIsPaused) {
                return;
            }
            postOnAnimation(mSetFrame);
            CameraView.this.invalidate();
            if (areAllPicturesTaken()) {
                onDoneTakingPhotosphere();
            }
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // updates the diameter, in case there were changes
        mViewDiameter = (float) Math.sqrt(getWidth() * getWidth() + getHeight() * getHeight());

        // sets the background to transparent
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);

        for (Picture pic : mPictures) {
            if (pic.isSaved()) {
                pic.draw(canvas);
            }
        }

        for (ReferencePoint point : mReferencePoints) {
            point.checkIn();
            point.drawPoint(canvas);
        }

        mPaint.setARGB(50, 255, 255, 255);
        mPaint.setStyle(Paint.Style.STROKE);


        mReferenceCircleDrawable.draw(canvas);

        mPictureFrame.draw(canvas);

        handleAlignmentAnimation(canvas);

        // sets the background color to a light grey
        canvas.drawColor(getResources().getColor(R.color.light_grey), PorterDuff.Mode.DST_OVER);
    }

    // these are the reference point (circles) on the screen that tell the user where to take
    // each picture
    public class ReferencePoint {
        private static final int HOVER_TIME_TO_CAPTURE = 500;

        private float[] mCoordinates;
        private boolean mIsPictureTaken; // whether the picture associated to this point has been
        private int mFramesInside; // counts the number of frames the user has been aiming this point
        private long mInitHoverTime;

        private PointType mType;

        public ReferencePoint(float[] coordinates, PointType type) {
            mCoordinates = coordinates;
            mType = type;
        }

        // every time a point is drawn, this checkIn method is called to tell whether the user
        // is aiming at this point, and if so for how long
        public int checkIn() {
            if (mIsPictureTaken) {
                return 0;
            }

            float[][] correctionMatrix = mOrientationManager.getCorrectionRotMatrix();
            if (!isFrontal(correctionMatrix)) {
                return 0;
            }

            float[] correctedLocation = MatrixUtils.multiply(mCoordinates, correctionMatrix);
            float[] p = project(correctedLocation);
            p[0] *= ZOOM_FACTOR * mViewDiameter;
            p[1] *= ZOOM_FACTOR * mViewDiameter;

            // if the aim is close enough to our point, we increment mFramesInside,
            // otherwise we reset it
            if (MatrixUtils.norm(p) < getResources().getDimension(R.dimen.dist_to_trigger)
                    && (getCurrentDeviceAlignment() == DeviceAlignment.CORRECT
                    || mType == PointType.SOUTH_POLE
                    || mType == PointType.NORTH_POLE)) {
                if (mFramesInside == 0) {
                    mInitHoverTime = System.currentTimeMillis();
                }
                mFramesInside++;

                // after 30 frames aiming the point, we take a picture
                if (System.currentTimeMillis() > mInitHoverTime + HOVER_TIME_TO_CAPTURE) {
                    mIsPictureTaken = true;
                    Log.i("info_", "taking picutre from " + Float.toString(mCoordinates[2]));
                    mVibrator.vibrate(20);
                    mPictures.add(mCameraController.takePicture("muito showz man", this));
                }

            } else {
                mFramesInside = 0;
            }

            return mFramesInside;
        }


        public void drawPoint(Canvas canvas) {
            if (mIsPictureTaken) {
                return;
            }

            float[][] correctionMatrix = mOrientationManager.getCorrectionRotMatrix();
            if (!isFrontal(correctionMatrix)) {
                return;
            }

            mPaint.setStyle(Paint.Style.FILL);

            float[] correctedLocation = MatrixUtils.multiply(mCoordinates, correctionMatrix);
            float[] p = project(correctedLocation);
            mPaint.setAlpha(255);
            mPaint.setColor(getResources().getColor(R.color.facebook_blue));
            float circleXCoord = p[0] * ZOOM_FACTOR * mViewDiameter + getWidth() / 2;
            float circleYCoord = p[1] * ZOOM_FACTOR * mViewDiameter + getHeight() / 2;

            float closenessFactor = Math.max(1, 0.5f + 0.9f / (1 + 4 * MatrixUtils.norm(p)));

            float pointRadius = getResources().getDimension(R.dimen.inner_circle_radius) * closenessFactor;

            mReferencePointDrawable.setBounds(
                    (int) (circleXCoord - pointRadius),
                    (int) (circleYCoord - pointRadius),
                    (int) (circleXCoord + pointRadius),
                    (int) (circleYCoord + pointRadius));
            mReferencePointDrawable.draw(canvas);


            mReferencePointFadeDrawable.setBounds(
                    (int) (circleXCoord - pointRadius),
                    (int) (circleYCoord - pointRadius),
                    (int) (circleXCoord + pointRadius),
                    (int) (circleYCoord + pointRadius));
            mReferencePointFadeDrawable.setAlpha(255 * mFramesInside / 30);
            mReferencePointFadeDrawable.draw(canvas);
        }

        private float[] project(float[] point) {
            return new float[]{point[0] / point[2], point[1] / point[2]};
        }

        private boolean isFrontal(float[][] correctionMatrix) {
            float[] correctLocation = MatrixUtils.multiply(mCoordinates, correctionMatrix);
            return correctLocation[2] > 0;
        }

        public void setIsPictureTaken(boolean isPictureTaken) {
            mIsPictureTaken = isPictureTaken;
        }
    }


    // these objects don't do anything for now. They will be placed at the intersection of
    //  neighboring and will be used as reference of where to look at when stitching the pictures
    private class MergingRectangle {
        private float[][] mVertices;
        private float[] mCenter;
        private float[][] mRotationMatrix;
        private float mWidth;
        private float mHeight;

        private Path mPath = new Path();

        MergingRectangle(float[][] rotationMatrix, float width, float height) {
            mRotationMatrix = rotationMatrix;

            mWidth = width;
            mHeight = height;

            float[][] vertices = new float[4][3];
            vertices[0] = new float[]{-height / 2, -width / 2, 1};
            vertices[1] = new float[]{height / 2, -width / 2, 1};
            vertices[2] = new float[]{-height / 2, width / 2, 1};
            vertices[3] = new float[]{height / 2, width / 2, 1};

            for (int i = 0; i < 4; i++) {
                vertices[i] = MatrixUtils.multiply(vertices[i], mRotationMatrix);
            }
            mVertices = vertices;

            float[] center = new float[]{0, 0, 1};
            mCenter = MatrixUtils.multiply(center, mRotationMatrix);
        }

        public float getWidth() {
            return mWidth;
        }

        public float getHeight() {
            return mHeight;
        }

        public void draw(Canvas canvas, Paint paint) {
            if (!isFrontal()) {
                return;
            }

            float[][] correctionMatrix = mOrientationManager.getCorrectionRotMatrix();

            float[][] rotatedVertices = new float[4][3];

            for (int i = 0; i < 4; i++) {
                rotatedVertices[i] = MatrixUtils.multiply(mVertices[i], correctionMatrix);
                if (rotatedVertices[i][2] <= 0.05) {
                    return;
                }
            }


            float[] p0 = project(rotatedVertices[0]);
            float[] p1 = project(rotatedVertices[1]);
            float[] p2 = project(rotatedVertices[2]);
            float[] p3 = project(rotatedVertices[3]);

            mPath = new Path();

            mPath.moveTo(
                    getWidth() / 2 + p0[0] * ZOOM_FACTOR * mViewDiameter,
                    getHeight() / 2 + p0[1] * ZOOM_FACTOR * mViewDiameter);
            mPath.lineTo(
                    getWidth() / 2 + p1[0] * ZOOM_FACTOR * mViewDiameter,
                    getHeight() / 2 + p1[1] * ZOOM_FACTOR * mViewDiameter);
            mPath.lineTo(
                    getWidth() / 2 + p3[0] * ZOOM_FACTOR * mViewDiameter,
                    getHeight() / 2 + p3[1] * ZOOM_FACTOR * mViewDiameter);
            mPath.lineTo(
                    getWidth() / 2 + p2[0] * ZOOM_FACTOR * mViewDiameter,
                    getHeight() / 2 + p2[1] * ZOOM_FACTOR * mViewDiameter);

            canvas.drawPath(mPath, paint);


        }

        private boolean isFrontal() {
            float[] correctLocation = MatrixUtils.multiply(
                    mCenter,
                    mOrientationManager.getCorrectionRotMatrix());
            return correctLocation[2] > 0.05;
        }


        private float[] project(float[] point) {
            return new float[]{point[0] / point[2], point[1] / point[2]};
        }

        public float[][] getRotationMatrix() {
            return mRotationMatrix;
        }

        public float[] getCenter() {
            return mCenter;
        }

        public float[][] getVertices() {
            return mVertices;
        }
    }

    // this method is necessary to pass on a new picture subclass to the CameraController
    public Picture getNewPicture() {
        return new Picture();
    }

    // when a picture is taken, we save it in this Picture object, which contains the picture
    // bitmap, its location, and tha reference point associated to it
    public class Picture {
        private Bitmap mBitmap;
        private float[][] mRotationMatrix;

        private boolean mIsSaved = false;

        private float mAbstractWidth, mAbstractHeight;
        private float[][] mVertices;

        private Matrix mMatrix = new Matrix();

        private ReferencePoint mReferencePoint;

        public float[][] getVertices() {
            return mVertices;
        }

        public void setVertices(Camera.Parameters cameraParameters) {
            if (mBitmap == null || mRotationMatrix == null) {
                throw new RuntimeException("Bitmap and rotation matrix must be both set before" +
                        " setting the vertices");
            }

            float[][] vertices = new float[4][3];
            // Horizontal means vertical for the camera, because portrait mode is its referential
            mAbstractWidth = 2 * (float) Math.tan((cameraParameters.getVerticalViewAngle()
                    * Math.PI / 180 / 2));
            mAbstractHeight = 2 * (float) Math.tan((cameraParameters.getHorizontalViewAngle()
                    * Math.PI / 180 / 2));

            vertices[0] = new float[]{-mAbstractWidth / 2, -mAbstractHeight / 2, 1};
            vertices[1] = new float[]{mAbstractWidth / 2, -mAbstractHeight / 2, 1};
            vertices[2] = new float[]{-mAbstractWidth / 2, mAbstractHeight / 2, 1};
            vertices[3] = new float[]{mAbstractWidth / 2, mAbstractHeight / 2, 1};
            for (int i = 0; i < 4; i++) {
                vertices[i] = MatrixUtils.multiply(vertices[i], mRotationMatrix);
            }
            mVertices = vertices;
        }


        //
        public Picture() {

        }

        public Bitmap getBitmap() {
            return mBitmap;
        }

        public float[][] getRotationMatrix() {
            return mRotationMatrix;
        }

        public boolean isSaved() {
            return mIsSaved;
        }

        public void setRotationMatrix(float[][] rotationMatrix) {
            mRotationMatrix = rotationMatrix;
        }

        public void setBitmap(Bitmap bitmap) {
            mBitmap = bitmap;
            Log.i("info_", "bitmap set");
        }

        public void setIsSaved(boolean isSaved) {
            mIsSaved = isSaved;
        }

        public ReferencePoint getReferencePoint() {
            return mReferencePoint;
        }

        public void setReferencePoint(ReferencePoint referencePoint) {
            mReferencePoint = referencePoint;
        }

        public float getAbstractWidth() {
            return mAbstractWidth;
        }

        public float getAbstractHeight() {
            return mAbstractHeight;
        }

        public void draw(Canvas canvas) {
            float[][] currentRotation = mOrientationManager.getCorrectionRotMatrix();
            float[][] vertices = getVertices();
            float[][] rotatedVertices = new float[4][3];

            // rotates the vertices to the device referential
            for (int i = 0; i < 4; i++) {
                rotatedVertices[i] = MatrixUtils.multiply(vertices[i], currentRotation);
            }

            // projects the vertices onto z = 1 and scales them onto the view size
            float[] p0 = project(rotatedVertices[0]);
            float[] p1 = project(rotatedVertices[1]);
            float[] p2 = project(rotatedVertices[2]);
            float[] p3 = project(rotatedVertices[3]);
            float[] quad = new float[]{
                    p0[0] * ZOOM_FACTOR * mViewDiameter, p0[1] * ZOOM_FACTOR * mViewDiameter,
                    p1[0] * ZOOM_FACTOR * mViewDiameter, p1[1] * ZOOM_FACTOR * mViewDiameter,
                    p2[0] * ZOOM_FACTOR * mViewDiameter, p2[1] * ZOOM_FACTOR * mViewDiameter,
                    p3[0] * ZOOM_FACTOR * mViewDiameter, p3[1] * ZOOM_FACTOR * mViewDiameter,
            };


            // this solves the bug of the disappearing images
            if (rotatedVertices[0][2] < 0) {
                for (int i = 0; i < 8; i++) {
                    quad = new float[]{
                            p3[0] * ZOOM_FACTOR * mViewDiameter, p3[1] * ZOOM_FACTOR * mViewDiameter,
                            p2[0] * ZOOM_FACTOR * mViewDiameter, p2[1] * ZOOM_FACTOR * mViewDiameter,
                            p1[0] * ZOOM_FACTOR * mViewDiameter, p1[1] * ZOOM_FACTOR * mViewDiameter,
                            p0[0] * ZOOM_FACTOR * mViewDiameter, p0[1] * ZOOM_FACTOR * mViewDiameter,
                    };
                }
            }

            if (Math.random() < 1.0 / 60) {
                Log.i("info__", Float.toString(rotatedVertices[0][0]) + ", " +
                        Float.toString(rotatedVertices[0][1]) + ", " +
                        Float.toString(rotatedVertices[0][2]));
            }

            // draws the quadrangular bitmap onto the quadrangle that the rotated vertices form
            if (isFrontal(currentRotation)) {// we reuse the same Matrix object to draw every rectangle
                mMatrix.setPolyToPoly(
                        new float[]{
                                0, 0,
                                getBitmap().getWidth(), 0,
                                0, getBitmap().getHeight(),
                                getBitmap().getWidth(), getBitmap().getHeight()
                        },
                        0,
                        quad,
                        0,
                        4);
                mMatrix.postTranslate(getWidth() / 2, getHeight() / 2);

                if (rotatedVertices[0][2] < 0) {
                    mMatrix.preTranslate(getBitmap().getWidth() / 2, getBitmap().getHeight() / 2);
                    mMatrix.preScale(-1, -1);
                    mMatrix.preTranslate(-getBitmap().getWidth() / 2, -getBitmap().getHeight() / 2);
                }

                Paint paint = new Paint();

                // we first draw our picture behind everything that has been drawn
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
                canvas.drawBitmap(getBitmap(), mMatrix, paint);

                // and then draw in front, with .5 alpha, to get a blend at the intersections
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
                paint.setAlpha(255 / 2);
                canvas.drawBitmap(getBitmap(), mMatrix, paint);
            }
        }

        private boolean isFrontal(float[][] currentRotation) {
            float[] vec = new float[]{0, 0, 1};
            vec = MatrixUtils.multiply(vec, getRotationMatrix());
            vec = MatrixUtils.multiply(vec, currentRotation);
            return vec[2] > 0;
        }


        private float[] project(float[] point) {
            return new float[]{point[0] / point[2], point[1] / point[2]};
        }

    }


    public List<Picture> getPictures() {
        return mPictures;
    }

    public PhotoSphereConstructor getPhotoSphereConstructor() {
        return mPhotoSphereConstructor;
    }

    public void startConstruction() {
        mPhotoSphereConstructor = PhotoSphereConstructor.getInstance(CameraView.this, DEFAULT_SPHERE_HEIGHT);
        for (Picture picture : mPictures) {
            mPhotoSphereConstructor.drawPicture();
        }
    }

    public void savePictureToFileWhenDone(String fileName) {
        if (mPhotoSphereConstructor == null) {
            return;
        }

        mPhotoSphereConstructor.setDestinationFile(fileName);
        if (mPhotoSphereConstructor.isConstructionDone()) {
            mPhotoSphereConstructor.savePictureToFile();
        }
    }


    private DeviceAlignment getCurrentDeviceAlignment() {
        float[] vector = MatrixUtils.multiply(new float[]{0, 0, 1}, mOrientationManager.getPositionRotMatrix());
        if (Math.abs(vector[0]) > 0.94) {
            return DeviceAlignment.CLOSE_TO_POLE;
        } else if (mOrientationManager.getDownwardsAngle() > MAXIMUM_ALLOWED_DEVICE_ROTATION) {
            return DeviceAlignment.LEFT;
        } else if (mOrientationManager.getDownwardsAngle() < -MAXIMUM_ALLOWED_DEVICE_ROTATION) {
            return DeviceAlignment.RIGHT;
        }

        return DeviceAlignment.CORRECT;
    }

    public boolean popPicture() {
        if (mPictures.size() == 0) {
            return false;
        }
        Picture picture = mPictures.remove(mPictures.size() - 1);
        picture.getReferencePoint().setIsPictureTaken(false);
        return true;
    }

    private boolean areAllPicturesTaken() {
        boolean areTaken = true;
        for (ReferencePoint referencePoint : mReferencePoints) {
            areTaken = areTaken && referencePoint.mIsPictureTaken;
        }
        return areTaken;
    }

    public void onDoneTakingPhotosphere() {
        // start construction and save to filename when done
    }

    public void handleAlignmentAnimation(Canvas canvas) {
        if (mAlignmentAnimationOldTime == 0) {
            mAlignmentAnimationOldTime = System.currentTimeMillis();
        }

        if (getCurrentDeviceAlignment() == DeviceAlignment.RIGHT) {
            mRightAlpha = Math.min(
                    255,
                    mRightAlpha + (int) (0.1 * Math.min(System.currentTimeMillis() - mAlignmentAnimationOldTime, 150)));
        } else {
            mRightAlpha = Math.max(
                    0,
                    mRightAlpha - (int) (0.3 * Math.min(System.currentTimeMillis() - mAlignmentAnimationOldTime, 150)));
        }

        if (getCurrentDeviceAlignment() == DeviceAlignment.LEFT) {
            mLeftAlpha = Math.min(
                    255,
                    mLeftAlpha + (int) (0.1 * Math.min(System.currentTimeMillis() - mAlignmentAnimationOldTime, 150)));
        } else {
            mLeftAlpha = Math.max(
                    0,
                    mLeftAlpha - (int) (0.3 * Math.min(System.currentTimeMillis() - mAlignmentAnimationOldTime, 150)));
        }

        mTurnLeftDrawable.setAlpha(mRightAlpha);
        // mTurnLeftDrawable.setBounds(getWidth() - 120, 0, getWidth(), 120);
        mTurnLeftDrawable.draw(canvas);

        mTurnRightDrawable.setAlpha(mLeftAlpha);
        // mTurnLeftDrawable.setBounds(getWidth() - 120, 0, getWidth(), 120);
        mTurnRightDrawable.draw(canvas);
    }

}
