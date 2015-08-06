// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.spherelib;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A single instance of this class is created by a SphereView or a CameraView to get orientation
 * information
 */
public class OrientationManager implements SensorEventListener {
    private SensorManager mSensorManager;

    private Sensor mSensor;

    private final Queue<float[]> mRecentQueue = new ArrayDeque<float[]>();
    private float[] mAverageRotationVector = new float[4];
    private final int mQueueSize = 1;


    public OrientationManager(Context context) {
        for (int i = 0; i < mQueueSize; i++) {
            mRecentQueue.add(new float[4]);
        }

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
    }

    public void start() {
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void stop() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] rotationVector = new float[]{event.values[0], event.values[1],
                event.values[2], event.values[3]};
        mRecentQueue.add(rotationVector);
        mAverageRotationVector = MatrixUtils.add(
                mAverageRotationVector,
                MatrixUtils.multiply(
                        MatrixUtils.subtract(rotationVector, mRecentQueue.remove()),
                        1f / mQueueSize));
    }

    // not used yet, will be useful for starting at the correct starting position when switching
    // from compass to manual mode
    public float[] getAngles() {
        float[][] rotation;
        float[] angles = new float[3];

        rotation = getCorrectionRotMatrix();
        SensorManager.getOrientation(MatrixUtils.rectangularToLinear(rotation), angles);

        return angles;
    }

    // gets an angle which is 0 when the device is oriented up
    public float getDownwardsAngle() {
        float[] unit = new float[] {0, 0, 1};
        float[] translatedUnit = new float[] {0, 0.1f, 1};

        unit = MatrixUtils.multiply(unit, getPositionRotMatrix());
        translatedUnit = MatrixUtils.multiply(translatedUnit, getPositionRotMatrix());


        float xRotation = getAngles()[2];
        float[][] horizontalRot = new float[][]{
                {1, 0, 0},
                {0, (float) Math.cos(xRotation), -(float) Math.sin(xRotation)},
                {0, (float) Math.sin(xRotation), (float) Math.cos(xRotation)}
        };

        unit = MatrixUtils.multiply(unit, horizontalRot);
        translatedUnit = MatrixUtils.multiply(translatedUnit, horizontalRot);

        unit = project(unit);
        translatedUnit = project(translatedUnit);

        float result = (float) (- Math.atan((unit[1] - translatedUnit[1])/(unit[0] - translatedUnit[0])));

        if (unit[0] > translatedUnit[0]) {
            result = result + (float) Math.PI;
        }

        return result;
    }

    private float[] project(float[] v) {
        return new float[] {v[0] / v[2], v[1] / v[2]};
    }

    // returns the rotation matrix that describes the current rotation of the device
    public float[][] getPositionRotMatrix() {
        // this is just the inverse of the correction rotation matrix
        // for a 3D rotation, the inverse is just the transpose
        return MatrixUtils.transpose(getCorrectionRotMatrix());
    }

    // returns the rotation matrix that compensates for the current position
    public float[][] getCorrectionRotMatrix() {

        float[] rotation = new float[9];
        SensorManager.getRotationMatrixFromVector(rotation, mAverageRotationVector);

        float[][] rotationMatrix = MatrixUtils.linearToRectangular(rotation, 3, 3);

        // two of the axis come with reversed orientation, so we correct it
        rotationMatrix = MatrixUtils.multiply (rotationMatrix, new float[][]{
                {1, 0, 0},
                {0, -1, 0},
                {0, 0, -1}
        });

        // these two rotation matrices give the correct starting point
        // (I don't fully understand what is the referential that the sensor uses,
        // but this rotation was easy to find just by trying out)
        float angle = (float) Math.PI / 2;
        rotationMatrix = MatrixUtils.multiply (
                new float[][]{
                        {1, 0, 0},
                        {0, (float) Math.cos(angle), -(float) Math.sin(angle)},
                        {0, (float) Math.sin(angle), (float) Math.cos(angle)}
                },
                rotationMatrix);

        angle = (float) - Math.PI / 2;
        rotationMatrix = MatrixUtils.multiply(
                new float[][]{
                        {(float) Math.cos(angle), -(float) Math.sin(angle), 0},
                        {(float) Math.sin(angle), (float) Math.cos(angle), 0},
                        {0, 0, 1}
                },
                rotationMatrix);

        return rotationMatrix;
    }

}
