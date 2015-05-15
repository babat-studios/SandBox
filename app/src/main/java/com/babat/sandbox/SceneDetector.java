package com.babat.sandbox;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class SceneDetector implements CameraView.CameraViewListener {

    protected static final String TAG = "SceneDetector";

    private static SceneDetector instance;

    private CameraPositionListener cpListener;

    private boolean busy = false;
    private boolean detected = false;
    private int fpd = 0;

    private FeatureDetector detector;
    private DescriptorExtractor extractor;

    private Mat crossMat = new Mat();
    private MatOfKeyPoint crossKeypoints = new MatOfKeyPoint();
    private Mat crossDescriptors = new Mat();

    private Mat rvec = new Mat();
    private Mat tvec = new Mat();

    private DetectWorker dWorker = new DetectWorker();


    static {
        System.loadLibrary("detector");
    }


    private SceneDetector(Context myContext)
    {
        detector = FeatureDetector.create(FeatureDetector.ORB);
        extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);

        try {
            InputStream crossIs = myContext.getAssets().open("crossCl.jpg");
            Bitmap crossBm = BitmapFactory.decodeStream(crossIs);
            Utils.bitmapToMat(crossBm, crossMat);
            detector.detect(crossMat, crossKeypoints);
            extractor.compute(crossMat, crossKeypoints, crossDescriptors);

            dWorker.start();
        } catch(IOException e) {
            e.printStackTrace(); //TODO: Crash!!!
        }
    }

    public static SceneDetector getInstance(Context myContext)
    {
        if (instance == null) {
            instance = new SceneDetector(myContext);
        }
        return instance;
    }

    public native boolean jniDetect(byte[] data, long cdesc, long rvec, long tvec);

    public void enableDetector()
    {
    }

    public void disableDetector()
    {
        detected = false;
    }

    public void addCameraPositionListener(CameraPositionListener cameraPositionListener)
    {
        cpListener = cameraPositionListener;
    }


    public synchronized void onCameraFrame(byte[] data) {
        if (detected) {
            if (cpListener != null) {
                cpListener.onCameraMoved(rvec, tvec);
            }
            detected = false;
        }
        dWorker.setData(data);
        fpd++;
    }


    private class DetectWorker extends Thread {

        private byte[] _data;

        public DetectWorker()
        {
        }

        public void setData(byte[] data)
        {
            _data = data;
        }

        public void run()
        {
            while (true) {
                if (_data == null) {
                    continue;
                }
                boolean d = jniDetect(_data,
                        crossDescriptors.getNativeObjAddr(),
                        rvec.getNativeObjAddr(),
                        tvec.getNativeObjAddr());
                if (d) {
                    Log.d(TAG, String.format("fpd %d", fpd));
                    fpd = 0;
                }
                detected = d;
            }
        }

    };

}