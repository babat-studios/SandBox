package com.babat.sandbox;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;


public class PositionDetector implements SensorEventListener {

    protected static final String TAG = "PositionDetector";
    private static final int SENSOR_DELAY = 41 * 1000;
    private static final int FROM_RADS_TO_DEGS = -57;

    private static PositionDetector instance;

    private MainActivity context;
    private SensorManager mSensorManager;
    private Sensor mRotationSensor;
    private Sensor mAccelerometerSensor;

    private float[] currentRotation;
    private float[] fixedRotation;

    private float[] currentTranslation = new float[3];

    private long timestamp =  System.currentTimeMillis();



    private PositionDetector(Context myContext)
    {
        Log.d(TAG, "Initializing motion sensors");
        context = (MainActivity) myContext;
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }


    public static PositionDetector getInstance(Context myContext)
    {
        if (instance == null) {
            instance = new PositionDetector(myContext);
        }
        return instance;
    }

    public void enableDetector()
    {
        mSensorManager.registerListener(this, mRotationSensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    public void disableDetector()
    {
        mSensorManager.unregisterListener(this);
    }

    public void fix()
    {
        if (currentRotation != null) {
            fixedRotation = new float[3];
            System.arraycopy(currentRotation, 0, fixedRotation, 0, 3);
        }
        currentTranslation[0] = 0;
        currentTranslation[1] = 0;
        currentTranslation[2] = 0;
    }


    public float[] angleChange()
    {
        float[] prev = new float[16];
        SensorManager.getRotationMatrixFromVector(prev, fixedRotation);

        float[] current = new float[16];
        SensorManager.getRotationMatrixFromVector(current, currentRotation);

        float[] angleChange = new float[3];
        SensorManager.getAngleChange(angleChange, current, prev);

        return angleChange;
    }

    public float[] positionChange()
    {
        return currentTranslation;
    }


    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        // TODO Auto-generated method stub
    }

    public void onSensorChanged(SensorEvent event)
    {
        if (event.sensor == mRotationSensor) {
            if (event.values.length > 4) {
                float[] truncatedVector = new float[4];
                System.arraycopy(event.values, 0, truncatedVector, 0, 4);
                updateRotation(truncatedVector);
            } else {
                updateRotation(event.values);
            }
        } else if (event.sensor == mAccelerometerSensor) {
            if (event.values.length > 4) {
                float[] truncatedVector = new float[4];
                System.arraycopy(event.values, 0, truncatedVector, 0, 4);
                updateTranslation(truncatedVector);
            } else {
                updateTranslation(event.values);
            }
        }
    }


    private void updateRotation(float[] vectors) {
        currentRotation = vectors;
    }

    private void updateTranslation(float[] vectors) {
        long tTime = System.currentTimeMillis();
        float diffSec = (tTime - timestamp)/1000.0f;
        timestamp = tTime;
        currentTranslation[0] += (vectors[0] * diffSec * diffSec);
        currentTranslation[1] += (vectors[1] * diffSec * diffSec);
        currentTranslation[2] += (vectors[2] * diffSec * diffSec);
        //Log.d(TAG, String.format("Position %f %f %f ", currentTranslation[0], currentTranslation[1], currentTranslation[2], diffSec));
    }

}
