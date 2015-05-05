package com.babat.vision;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SceneDetector {

    protected static final String TAG = "Detector";

    private static SceneDetector instance;
    private static MainActivity context;

    private boolean busy = false;
    private boolean detected = false;

    private FeatureDetector detector;
    private DescriptorExtractor extractor;
    private DescriptorMatcher matcher;

    private Mat crossMat = new Mat();
    private MatOfKeyPoint crossKeypoints = new MatOfKeyPoint();
    private Mat crossDescriptors = new Mat();


    private SceneDetector(Context myContext)
    {
        context = (MainActivity) myContext;
        detector = FeatureDetector.create(FeatureDetector.ORB);
        extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        try {
            InputStream crossIs = myContext.getAssets().open("crossCl.jpg");
            Bitmap crossBm = BitmapFactory.decodeStream(crossIs);
            Utils.bitmapToMat(crossBm, crossMat);
            detector.detect(crossMat, crossKeypoints);
            extractor.compute(crossMat, crossKeypoints, crossDescriptors);
        } catch(IOException e) {
            e.printStackTrace(); //TODO: Crash!!!
        }
    }


    private void calibrate(Mat img, List<Point> crosses)
    {
        Log.d(TAG, "Calibrating camera");

        detected = true;

        Size imgSize = img.size();

        List<Double> xs = new ArrayList<Double>();
        List<Double> ys = new ArrayList<Double>();
        for (Point pnt : crosses) {
            xs.add(pnt.x);
            ys.add(pnt.y);
        }
        Collections.sort(xs);
        Collections.sort(ys);

        Mat objectPointView = new Mat(4,3,5);
        Mat imagePointView = new Mat(4,2,5);
        for (Point pnt : crosses) {
            int xIdx = xs.indexOf(pnt.x);
            int yIdx = ys.indexOf(pnt.y);

            int mIdx = 3;
            if (xIdx < 2 && yIdx < 2) {
                mIdx = 0;
                objectPointView.put(mIdx,0,imgSize.width/2 - 300);
                objectPointView.put(mIdx,1,imgSize.height/2 - 300);
            } else if (xIdx >= 2 && yIdx < 2) {
                mIdx = 1;
                objectPointView.put(mIdx,0,imgSize.width/2 + 300);
                objectPointView.put(mIdx,1,imgSize.height/2 - 300);
            } else if (xIdx >= 2 && yIdx >= 2) {
                mIdx = 2;
                objectPointView.put(mIdx,0,imgSize.width/2 + 300);
                objectPointView.put(mIdx,1,imgSize.height/2 + 300);
            } else {
                objectPointView.put(mIdx,0,imgSize.width/2 - 300);
                objectPointView.put(mIdx,1,imgSize.height/2 + 300);
            }
            objectPointView.put(mIdx,2,0);

            imagePointView.put(mIdx,0,pnt.x);
            imagePointView.put(mIdx,1,pnt.y);
        }

        // ADJUSTMENT
//        imagePointView.put(3,0,
//                imagePointView.get(3,0)[0] + ( imagePointView.get(1,0)[0] - imagePointView.get(0,0)[0]));
//        imagePointView.put(3,1,
//                imagePointView.get(3,1)[0] + ( imagePointView.get(1,1)[0] - imagePointView.get(0,1)[0]));

        //DEBUG
//        for (int tIdx = 0; tIdx < 4; tIdx++) {
//            Log.d(TAG,
//                    String.format("test_a %d :: %f %f", tIdx, objectPointView.get(tIdx,0)[0], objectPointView.get(tIdx,1)[0])
//            );
//            Log.d(TAG,
//                    String.format("test_b %d :: %f %f", tIdx, imagePointView.get(tIdx,0)[0], imagePointView.get(tIdx,1)[0])
//            );
//        }

        List<Mat> objectPoints = new ArrayList<Mat>();
        objectPoints.add(objectPointView);
        List<Mat> imagePoints = new ArrayList<Mat>();
        imagePoints.add(imagePointView);
        Mat cameraMatrix = new Mat();
        Mat distCoefs = new Mat();
        List<Mat> rvecs = new ArrayList<Mat>();
        List<Mat> tvecs = new ArrayList<Mat>();

        Calib3d.calibrateCamera(objectPoints, imagePoints, imgSize, cameraMatrix, distCoefs, rvecs, tvecs);

        for (int rIdx = 0; rIdx < cameraMatrix.rows(); rIdx++) {
            for (int cIdx = 0; cIdx < cameraMatrix.cols(); cIdx++) {
                double[] val = cameraMatrix.get(rIdx, cIdx);
                Log.d(TAG, String.format("Camera matrix %f at %d %d", val[0], rIdx, cIdx ));
            }
        }
        for (int rIdx = 0; rIdx < tvecs.get(0).rows(); rIdx++) {
            for (int cIdx = 0; cIdx < tvecs.get(0).cols(); cIdx++) {
                double[] val = tvecs.get(0).get(rIdx, cIdx);
                Log.d(TAG, String.format("tvec matrix %f at %d %d", val[0], rIdx, cIdx ));
            }
        }
        for (int rIdx = 0; rIdx < rvecs.get(0).rows(); rIdx++) {
            for (int cIdx = 0; cIdx < rvecs.get(0).cols(); cIdx++) {
                double[] val = rvecs.get(0).get(rIdx, cIdx);
                Log.d(TAG, String.format("rvec matrix %f at %d %d", val[0], rIdx, cIdx ));
            }
        }

        context.render(rvecs.get(0), tvecs.get(0));
    }


    private void release()
    {
        busy = false;
    }



    private class Worker extends Thread {

        private SceneDetector sceneDetector;
        private Mat image;

        public Worker(SceneDetector det, Mat img)
        {
            sceneDetector = det;
            image = img;
        }

        public void run()
        {
            Log.d(TAG, String.format("Detecting scene [%d]", getId()));

            MatOfKeyPoint keypoints = new MatOfKeyPoint();
            sceneDetector.detector.detect(image, keypoints);

            Mat descriptors  = new Mat();
            sceneDetector.extractor.compute(image, keypoints, descriptors);

            if (descriptors.cols() + descriptors.rows() > 0) {
                MatOfDMatch matches = new MatOfDMatch();
                matcher.match(descriptors, crossDescriptors, matches);
                List<DMatch> matchList = matches.toList();

                List<KeyPoint> kpnt = keypoints.toList();
                List<Point>  pnts = new ArrayList<Point>();
                for (DMatch dm : matchList) {
                    if (dm.distance < 50) { //hardcoded
                        Point pnt = kpnt.get(dm.queryIdx).pt;
                        pnts.add(pnt);
                    }
                }

                Map<Point, Integer> crosses = new HashMap<Point, Integer>();
                for (Point pnt : pnts) {
                    boolean addNew = true;
                    for (Point crPnt : crosses.keySet()) {
                        double dist = Math.abs(pnt.x-crPnt.x) + Math.abs(pnt.y-crPnt.y);
                        if (dist < 35) { //hardcoded
                            addNew = false;
                            crosses.put(crPnt, crosses.get(crPnt)+1);
                            break;
                        }
                    }
                    if (addNew) {
                        crosses.put(pnt, 1);
                    }
                }

                List<Point> resList = new ArrayList<Point>();
                for (Point pnt : crosses.keySet()) {
                    if (crosses.get(pnt) > 3) { //hardcoded
                        resList.add(pnt);
                        Log.d(TAG, String.format("Cross found at (%f, %f) [%d]", pnt.x, pnt.y, getId()));
                    }
                }

                if (resList.size() == 4) {
                    sceneDetector.calibrate(image, resList);
                }
            }

            sceneDetector.release();
        }

    };


    static SceneDetector getInstance(Context myContext)
    {
        if (instance == null) {
            instance = new SceneDetector(myContext);
        }
        return instance;
    }

    synchronized void compute(Mat inImage)
    {
        if (!detected && !busy) {
            busy = true;
            new Worker(this, inImage).start();
        }
    }

}