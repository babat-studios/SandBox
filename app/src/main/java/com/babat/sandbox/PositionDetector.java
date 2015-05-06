package com.babat.sandbox;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;


public class PositionDetector implements SensorEventListener {

    protected static final String TAG = "PositionDetector";
    private static final int SENSOR_DELAY = 500 * 1000; // 500ms
    private static final int FROM_RADS_TO_DEGS = -57;

    private static PositionDetector instance;

    private MainActivity context;
    private SensorManager mSensorManager;
    private Sensor mRotationSensor;


    public static PositionDetector getInstance(Context myContext)
    {
        if (instance == null) {
            instance = new PositionDetector(myContext);
        }
        return instance;
    }

    public void enableDetector() {
        mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY);
    }

    public void disableDetector() {
        mSensorManager.unregisterListener(this);
    }

    private PositionDetector(Context myContext) {
        Log.d(TAG, "Initializing motion sensors");
        context = (MainActivity) myContext;
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mRotationSensor) {
            if (event.values.length > 4) {
                float[] truncatedRotationVector = new float[4];
                System.arraycopy(event.values, 0, truncatedRotationVector, 0, 4);
                update(truncatedRotationVector);
            } else {
                update(event.values);
            }
        }
    }


    private void update(float[] vectors) {
//        float[] rotationMatrix = new float[9];
//        SensorManager.getRotationMatrixFromVector(rotationMatrix, vectors);
//        int worldAxisX = SensorManager.AXIS_X;
//        int worldAxisZ = SensorManager.AXIS_Z;
//        float[] adjustedRotationMatrix = new float[9];
//        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisX, worldAxisZ, adjustedRotationMatrix);
//        float[] orientation = new float[3];
//        SensorManager.getOrientation(adjustedRotationMatrix, orientation);
//        float pitch = orientation[1] * FROM_RADS_TO_DEGS;
//        float roll = orientation[2] * FROM_RADS_TO_DEGS;
        Log.d(TAG, String.format("%f, %f, %f", vectors[0], vectors[1], vectors[2]));
    }

}
