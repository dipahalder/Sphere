// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.sphereviewer;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Gallery;

public class SphereView extends View {
    private static final int GRID_WIDTH = 30;
    private static final int GRID_HEIGHT = 30;

    private static final float MIN_ZOOM = 0.18f;
    private static final float INITIAL_ZOOM = 0.4f;
    private static final float MAX_ZOOM = 0.8f;

    private static final float FRICTION = 0.0006f;
    private static final float VELOCITY_FACTOR = 4f;
    private float mXRotation;
    private float mYRotation;
    private float mPreviousFrameX;
    private float mPreviousFrameY;
    private float mVelocityX;
    private float mVelocityY;
    private float oldX;
    private float oldY;
    private boolean mWaitNewFinger;
    private long mOldTime;

    private boolean mIsOnCompassMode = true;
    private boolean mIsFingerOnScreen = false;


    Sphere mSphere;
    OrientationManager mOrientationManager;
    Context mContext;

    ScaleGestureDetector mScaleGestureDetector;
    GestureDetector mDoubleTapDetector;

    boolean mIsDoubleClickSwitchAllowed = true;
    boolean mIsZoomAllowed = true;

    public SphereView(Context context, Bitmap sourceBitmap) {
        super(context);
        mContext = context;
        mSphere = new Sphere(GRID_WIDTH, GRID_HEIGHT, sourceBitmap);
        mSphere.setZoomFactor(INITIAL_ZOOM);

        mOldTime = System.currentTimeMillis();

        mOrientationManager = new OrientationManager(mContext);

        mScaleGestureDetector = new ScaleGestureDetector(mContext, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (!mIsZoomAllowed) {
                    return false;
                }
                float newZoomFactor = mSphere.getZoomFactor() * detector.getCurrentSpan() / detector.getPreviousSpan();
                // restrict zooming to allowed zoom interval
                if (newZoomFactor >= MIN_ZOOM && newZoomFactor <= MAX_ZOOM) {
                    mSphere.setZoomFactor(newZoomFactor);
                }
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {

            }
        });

        mDoubleTapDetector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent event) {
                if (!mIsDoubleClickSwitchAllowed) {
                    return false;
                }
                mIsOnCompassMode = !mIsOnCompassMode;
                return true;
            }

        });


        this.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mScaleGestureDetector.onTouchEvent(event);
                mDoubleTapDetector.onTouchEvent(event);
                // We only allow these methods when in manual mode
                if (mIsOnCompassMode) {
                    return true;
                }
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: {
                        oldX = event.getX();
                        oldY = event.getY();
                        mWaitNewFinger = false;
                        mIsFingerOnScreen = true;
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        // We won't rotate the picture during scaling because it creates ugly visual effects
                        // This mWaitNewFinger exists to handle the case in which a finger is
                        // replaced by other after scaling, by forcing oldX and oldY to be updated
                        // otherwise the program will just think the user requested a big rotation,
                        // for confusing the coordinates of the two fingers
                        if (!mScaleGestureDetector.isInProgress() && !mWaitNewFinger) {
                            mXRotation -= 2 * ((event.getX() - oldX)) / getWidth();
                            float tempYRotation = mYRotation + 2 * ((event.getY() - oldY)) / getHeight();
                            if (Math.abs(tempYRotation) <= Math.PI / 2) {
                                mYRotation += 2 * ((event.getY() - oldY)) / getHeight();
                            }
                        }

                        oldX = event.getX();
                        oldY = event.getY();
                        mWaitNewFinger = false;
                        mIsFingerOnScreen = true;
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        mWaitNewFinger = true;
                        mIsFingerOnScreen = false;
                        break;
                    }
                    case MotionEvent.ACTION_POINTER_UP: {
                        mWaitNewFinger = true;
                        break;
                    }
                }
                return true;
            }
        });


    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mSphere.drawMosaic(canvas);
    }


    @Override
    protected void onAttachedToWindow() {
        postOnAnimation(mSetFrame);
        if (getLayoutParams() == null) {
            setLayoutParams(new Gallery.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }


    @Override
    protected void onDetachedFromWindow() {

    }

    private final Runnable mSetFrame = new Runnable() {
        public void run() {

            postOnAnimation(mSetFrame);

            if (mIsOnCompassMode) {
                mSphere.rotate(mOrientationManager.getCorrectionRotMatrix());
            } else {
                handleAnimatedCoordinates();
                Log.i("info_", Integer.toString(mContext.getResources().getConfiguration().orientation));
                mSphere.rotate(mXRotation, mYRotation);
            }

            SphereView.this.invalidate();
        }
    };

    public void handleAnimatedCoordinates() {
        if (mIsFingerOnScreen) {
            mVelocityX = VELOCITY_FACTOR * (mXRotation - mPreviousFrameX) / (System.currentTimeMillis() - mOldTime);
            mVelocityY = VELOCITY_FACTOR * (mYRotation - mPreviousFrameY) / (System.currentTimeMillis() - mOldTime);
            mPreviousFrameX = mXRotation;
            mPreviousFrameY = mYRotation;
            mOldTime = System.currentTimeMillis();
        } else {
            mXRotation += mVelocityX;
            float yRotation = mYRotation + mVelocityY;
            if (Math.abs(yRotation) < Math.PI / 2) {
                mYRotation += mVelocityY;
            }
            mVelocityX = Math.max(Math.abs(mVelocityX) - FRICTION, 0) * Math.signum(mVelocityX);
            mVelocityY = Math.max(Math.abs(mVelocityY) - FRICTION, 0) * Math.signum(mVelocityY);
        }
    }


    public void setIsOnCompassMode(boolean isOnCompassMode) {
        mIsOnCompassMode = isOnCompassMode;
    }

    public void setIsDoubleClickSwitchAllowed(boolean doubleClickSwitchAllowed) {
        this.mIsDoubleClickSwitchAllowed = doubleClickSwitchAllowed;
    }

    public void setIsZoomAllowed(boolean zoomAllowed) {
        this.mIsZoomAllowed = zoomAllowed;
    }

    private class Sphere {
        float[][][] mVertices;
        float[][][] mRotatedVertices;

        int mGridWidth, mGridHeight;
        float[] mHorizontalBreakingPoints;
        float[] mVerticalBreakingPoints;

        private int mPhotoSphereWidth;
        private int mPhotoSphereHeight;
        private Bitmap[][] mMosaic;

        private float mZoomFactor = 0.4f;

        private final Matrix mMatrix = new Matrix();

        public Sphere(int gridWidth, int gridHeight, Bitmap bitmap) {
            mGridWidth = gridWidth;
            mGridHeight = gridHeight;
            mVertices = new float[gridWidth][gridHeight][3];
            mRotatedVertices = new float[gridWidth][gridHeight][3];

            mPhotoSphereWidth = bitmap.getWidth();
            mPhotoSphereHeight = bitmap.getHeight();
            setMosaic(bitmap);

            // Creates the mVertices[][] array, that contains the points that tell us where
            // each rectangle of the mosaic will be mapped on the sphere
            // Notice that this choice of points doesn't look like a regular polyhedron,
            // but instead it looks like meridians and parallels of a globe, so they work out
            // perfectly to undistort the bitmap representation of a sphere
            for (int j = 0; j < gridHeight; j++) {
                for (int i = 0; i < gridWidth; i++) {
                    float xAngle = (float) (2 * Math.PI * mHorizontalBreakingPoints[i]);
                    float yAngle = (float) (Math.PI * (mVerticalBreakingPoints[j] - 1.0f / 2));
                    float vertex[] = {
                            (float) Math.sin(yAngle),
                            (float) (Math.cos(xAngle) * Math.cos(yAngle)),
                            (float) (Math.sin(xAngle) * Math.cos(yAngle))
                    };
                    mVertices[i][j] = vertex;
                }
            }
            rotate(0, 0);
        }

        // Gets a rectangular bitmap and draws it in a general quadrangle via the unique
        // appropriate projection
        private void drawBitmapInQuadrangle(Bitmap bitmap, Canvas canvas, float[] points) {
            // we reuse the same Matrix object to draw every rectangle
            mMatrix.setPolyToPoly(
                new float[]{
                            0, 0,
                            bitmap.getWidth(), 0,
                            0, bitmap.getHeight(),
                            bitmap.getWidth(), bitmap.getHeight()
                    },
                0,
                points,
                0,
                4);
            mMatrix.postTranslate(getWidth() / 2, getHeight() / 2);
            canvas.drawBitmap(bitmap, mMatrix, null);
        }

        // In order to generate a 2D view of our 3D scene, we project our scene onto the plane z = 1
        // We then expand the view with the appropriate zoom factor so it fills the view
        private float[] project(float[] point) {
            float diameter = (float) Math.sqrt(getWidth() * getWidth() + getHeight() * getHeight());
            return new float[]{
                    mZoomFactor * diameter* point[0] / point[2],
                    mZoomFactor * diameter * point[1] / point[2]
            };
        }

        // Slices the picture in a Bitmap[][] mosaic
        public void setMosaic(Bitmap bitmap) {
            setBreakingPoints();
            mMosaic = new Bitmap[mGridWidth][mGridHeight];
            for (int i = 0; i < mGridWidth; i++) {
                for (int j = 0; j < mGridHeight - 1; j++) {
                    mMosaic[i][j] = Bitmap.createBitmap(
                        bitmap,
                        (int) (mPhotoSphereWidth * mHorizontalBreakingPoints[i]),
                        (int) (mPhotoSphereHeight * mVerticalBreakingPoints[j]),
                        (int) (mPhotoSphereWidth * (mHorizontalBreakingPoints[i + 1]
                                    - mHorizontalBreakingPoints[i])),
                        (int) (mPhotoSphereHeight * (mVerticalBreakingPoints[j + 1]
                                    - mVerticalBreakingPoints[j])));
                }
            }
        }

        // Draws the 2D view of the sphere onto the canvas
        public void drawMosaic(Canvas canvas) {
            if (mMosaic == null) {
                throw new RuntimeException("No mosaic is set");
            }

            for (int j = 0; j < mGridHeight - 1; j++) {
                for (int i = 0; i < mGridWidth; i++) {
                    if (isEntirelyFrontal(i, j)) {
                        float[] p1 = project(mRotatedVertices[i][j]);
                        float[] p2 = project(mRotatedVertices[(i + 1) % mGridWidth][j]);
                        float[] p3 = project(mRotatedVertices[(i + 1) % mGridWidth][j + 1]);
                        float[] p4 = project(mRotatedVertices[i][j + 1]);
                        float[] quad = new float[]{p1[0], p1[1], p2[0], p2[1], p4[0], p4[1], p3[0], p3[1]};
                        drawBitmapInQuadrangle(mMosaic[i][j], canvas, quad);
                    }
                }
            }
        }

        // creates arrays saying in which proportion to break down the initial bitmap and where to
        // create the matching points on the sphere
        // for now the breaking is uniform, but later this can have smarter breaking points
        private void setBreakingPoints() {
            mHorizontalBreakingPoints = new float[mGridWidth + 1];
            mVerticalBreakingPoints = new float[mGridHeight];
            for (int i = 0; i < mGridWidth + 1; i++) {
                mHorizontalBreakingPoints[i] = i * 1.0f / mGridWidth;
            }

            for (int j = 0; j < mGridHeight; j++) {
                mVerticalBreakingPoints[j] = 0.0005f + j * 0.999f / (mGridHeight - 1);
            }
        }

        // creates a call to rotate() based only on horizontal and vertical rotation float parameters
        public void rotate(float xRotation, float yRotation) {
            float[][] rotMatrix = new float[][]{
                    {1, 0, 0},
                    {0, (float) Math.cos(xRotation), -(float) Math.sin(xRotation)},
                    {0, (float) Math.sin(xRotation), (float) Math.cos(xRotation)}
            };

            rotMatrix = MatrixUtils.multiply(rotMatrix, new float[][]{
                    {(float) Math.cos(yRotation), 0, -(float) Math.sin(yRotation)},
                    {0, 1, 0},
                    {(float) Math.sin(yRotation), 0, (float) Math.cos(yRotation)}
            });

            double angle = - Math.PI / 2;
            rotMatrix = MatrixUtils.multiply(rotMatrix, new float[][]{
                    {(float) Math.cos(angle), -(float) Math.sin(angle), 0},
                    {(float) Math.sin(angle), (float) Math.cos(angle), 0},
                    {0, 0, 1}
            });
            rotate(rotMatrix);
        }

        // Sets the rotated vertices to be in a rotation of rotMatrix
        // This rotation is not cumulative
        public void rotate(float[][] rotMatrix) {
            for (int j = 0; j < mGridHeight; j++) {
                for (int i = 0; i < mGridWidth; i++) {
                    mRotatedVertices[i][j] = MatrixUtils.multiply(mVertices[i][j], rotMatrix);
                }
            }
        }

        // Checks if rotated rectangle is entiraly in the region z > 0
        private boolean isEntirelyFrontal(int i, int j) {
            // To avoid buggy projections at infinity we consider to be in front of us only
            // points that have a z coordinate greater than some epsilon > 0
            float epsilon = 0.10f;
            return (mRotatedVertices[i][j][2] > epsilon &&
                    mRotatedVertices[(i + 1) % mGridWidth][j][2] > epsilon &&
                    mRotatedVertices[(i + 1) % mGridWidth][j + 1][2] > epsilon &&
                    mRotatedVertices[i][j + 1][2] > epsilon);
        }

        public float getZoomFactor() {
            return mZoomFactor;
        }

        public void setZoomFactor(float zoomFactor) {
            mZoomFactor = zoomFactor;
        }
    }

}
