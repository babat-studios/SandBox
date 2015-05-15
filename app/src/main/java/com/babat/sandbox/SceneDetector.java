package com.babat.sandbox;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SceneDetector implements CameraView.CameraViewListener {

    protected static final String TAG = "SceneDetector";

    private static SceneDetector instance;

    private MainActivity context;
    private PositionDetector positionDetector;

    private CameraPositionListener cpListener;

    private boolean busy = false;
    private boolean detected = false;

    private FeatureDetector detector;
    private DescriptorExtractor extractor;
    private DescriptorMatcher matcher;

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
        context = (MainActivity) myContext;

        positionDetector = PositionDetector.getInstance(myContext);
        positionDetector.enableDetector();

        detector = FeatureDetector.create(FeatureDetector.ORB);
        extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);

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
        positionDetector.enableDetector();
    }

    public void disableDetector()
    {
        detected = false;
        positionDetector.disableDetector();
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
        }
        dWorker.setData(data);
    }


    private class DetectWorker extends Thread {

        private byte[] _data;

        public DetectWorker()
        {
        }

        public void setData(byte[] data)
        {
            if (!busy) {
                _data = data;
            }
        }

        public void run()
        {
            while (true) {
                if (_data == null) {
                    continue;
                }
                detected = jniDetect(_data,
                        crossDescriptors.getNativeObjAddr(),
                        rvec.getNativeObjAddr(),
                        tvec.getNativeObjAddr());

            }
        }

    };

}