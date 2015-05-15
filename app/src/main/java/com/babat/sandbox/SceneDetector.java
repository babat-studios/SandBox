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

    public native Point[] test(byte[] data, long cdesc, long rvec, long tvec);

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
        if (detected) { //listen to position detector

            //                float[] angleChange = positionDetector.angleChange();
            //
            //                Mat temp = rvec.clone();
            //
            //                double[]buf = new double[1];
            //                buf[0] = rvec.get(0, 0)[0] + (double)angleChange[2];
            //                temp.put(0, 0, buf);
            //
            //                buf = new double[1];
            //                buf[0] = rvec.get(1, 0)[0] -(double)angleChange[0];
            //                temp.put(1,0,buf);
            //
            //                buf = new double[1];
            //                buf[0] = rvec.get(2, 0)[0] + (double)angleChange[1];
            //                temp.put(2,0,buf);
            //
            //                float[] pos = positionDetector.positionChange();
            //
            //                Mat temp2 = rvec.clone();
            //
            //                buf = new double[1];
            //                buf[0] = tvec.get(0, 0)[0] + (double)pos[0];
            //                temp2.put(0, 0, buf);
            //
            //                buf = new double[1];
            //                buf[0] = tvec.get(1, 0)[0] -(double)pos[1];
            //                temp2.put(1,0,buf);
            //
            //                buf = new double[1];
            //                buf[0] = tvec.get(2, 0)[0] + (double)pos[2]; ;
            //                temp2.put(2,0,buf);
            //
            //                Log.d("GOOO", String.format("%f %f %f - %f %f %f - %f %f %f",
            //                        tvec.get(0, 0)[0],
            //                        tvec.get(1, 0)[0],
            //                        tvec.get(2, 0)[0],
            //                        pos[0],
            //                        pos[1],
            //                        pos[2],
            //                        temp2.get(0, 0)[0],
            //                        temp2.get(1, 0)[0],
            //                        temp2.get(2, 0)[0]));
            if (cpListener != null) {
                cpListener.onCameraMoved(rvec, tvec);
            }
        }
        //positionDetector.fix();
        dWorker.setData(data);
    }


    private class DetectWorker extends Thread {

        private byte[] _data;

        Size imgSize = new Size(1920, 1080);
        MatOfDouble distCoefs = new MatOfDouble();
        Mat cameraMatrix = new Mat(3,3,5);

        public DetectWorker()
        {
            double focal_mm = 3.97;
            double sensor_width_mm = 4.54;
            double focal_px = (focal_mm/sensor_width_mm) * imgSize.width;
            cameraMatrix.put(0, 0, focal_px);
            cameraMatrix.put(0, 1, 0);
            cameraMatrix.put(0, 2, imgSize.width/2);
            cameraMatrix.put(1, 0, 0);
            cameraMatrix.put(1, 1, focal_px);
            cameraMatrix.put(1, 2, imgSize.height/2);
            cameraMatrix.put(2, 0, 0);
            cameraMatrix.put(2, 1, 0);
            cameraMatrix.put(2, 2, 1);
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

                Point[] pnts = test(_data, crossDescriptors.getNativeObjAddr(), rvec.getNativeObjAddr(), tvec.getNativeObjAddr());

                if (pnts.length == 4) {
                    detected = true;
                }
            }
        }

    };

}