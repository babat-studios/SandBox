package com.babat.sandbox;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
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
import org.opencv.core.Core;

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


public class SceneDetector implements CameraView.CameraViewListener {

    protected static final String TAG = "Detector";

    private static SceneDetector instance;

    private MainActivity context;

    private boolean busy = false;
    private boolean detected = false;

    private FeatureDetector detector;
    private DescriptorExtractor extractor;
    private DescriptorMatcher matcher;

    private Mat crossMat = new Mat();
    private MatOfKeyPoint crossKeypoints = new MatOfKeyPoint();
    private Mat crossDescriptors = new Mat();


    public static SceneDetector getInstance(Context myContext)
    {
        if (instance == null) {
            instance = new SceneDetector(myContext);
        }
        return instance;
    }


    public void onCameraFrame(byte[] data)
    {
        if (!detected && !busy) {
            busy = true;
            DetectWorker dWorker = new DetectWorker(this, data);
            dWorker.start();
        }
    }


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

        List<Point3> objectPnts = new ArrayList<Point3>();
        List<Point> imagePnts = new ArrayList<Point>();
        for (Point pnt : crosses) {
            int xIdx = xs.indexOf(pnt.x);
            int yIdx = ys.indexOf(pnt.y);

            if (xIdx < 2 && yIdx < 2) {
                objectPnts.add(new Point3(-1, -1, 0));
            } else if (xIdx >= 2 && yIdx < 2) {
                objectPnts.add(new Point3(1, -1, 0));
            } else if (xIdx >= 2 && yIdx >= 2) {
                objectPnts.add(new Point3(1, 1, 0));
            } else {
                objectPnts.add(new Point3(-1, 1, 0));
            }
            imagePnts.add(new Point(pnt.x, pnt.y));
        }
//        // ADJUSTMENT
//        imagePointView.put(3,0,
//                imagePointView.get(2,0)[0] - ( imagePointView.get(1,0)[0] - imagePointView.get(0,0)[0]));
//        imagePointView.put(3,1,
//                imagePointView.get(2,1)[0] - ( imagePointView.get(1,1)[0] - imagePointView.get(0,1)[0]));
        MatOfPoint3f objectPointView = new MatOfPoint3f();
        objectPointView.fromList(objectPnts);
        MatOfPoint2f imagePointView = new MatOfPoint2f();
        imagePointView.fromList(imagePnts);




        List<MatOfPoint3f> objectPoints = new ArrayList<MatOfPoint3f>();
        objectPoints.add(objectPointView);
        List<MatOfPoint2f> imagePoints = new ArrayList<MatOfPoint2f>();
        imagePoints.add(imagePointView);

        Mat cameraMatrix = new Mat(3,3,5); //Calib3d.initCameraMatrix2D(objectPoints, imagePoints, imgSize);
        cameraMatrix.put(0,0,1697);
        cameraMatrix.put(0,1,0);
        cameraMatrix.put(0,2,960);
        cameraMatrix.put(1,0,0);
        cameraMatrix.put(1,1,1697);
        cameraMatrix.put(1,2,540);
        cameraMatrix.put(2,0,0);
        cameraMatrix.put(2,1,0);
        cameraMatrix.put(2,2,1);

        for (int rIdx = 0; rIdx < cameraMatrix.rows(); rIdx++) {
            for (int cIdx = 0; cIdx < cameraMatrix.cols(); cIdx++) {
                double[] val = cameraMatrix.get(rIdx, cIdx);
                Log.d(TAG, String.format("Camera matrix %f at %d %d", val[0], rIdx, cIdx ));
            }
        }


        Mat distCoefs = new Mat();
        List<Mat> rvecs = new ArrayList<Mat>();
        List<Mat> tvecs = new ArrayList<Mat>();

        Mat obj1 = new Mat();
        Mat img1 = new Mat();

        objectPointView.convertTo(obj1, 5);
        imagePointView.convertTo(img1, 5);

        //        //DEBUG
//        for (int tIdx = 0; tIdx < 4; tIdx++) {
//            Log.d(TAG,  String.format("test_a %d :: %f %f", tIdx, obj1.get(tIdx,0)[0], obj1.get(tIdx,1)[0]) );
//            Log.d(TAG,  String.format("test_b %d :: %f %f", tIdx, img1.get(tIdx,0)[0], img1.get(tIdx,1)[0]) );
//        }

        List<Mat> obj1List = new ArrayList<Mat>();
        obj1List.add(obj1);
        List<Mat> img1List = new ArrayList<Mat>();
        img1List.add(img1);


        Calib3d.calibrateCamera(obj1List, img1List, imgSize, cameraMatrix, distCoefs, rvecs, tvecs, Calib3d.CALIB_USE_INTRINSIC_GUESS);

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

        Mat rodr = new Mat();
        Calib3d.Rodrigues(rvecs.get(0), rodr);
        context.render(rodr, tvecs.get(0));
    }


    private class DetectWorker extends Thread {

        private SceneDetector sceneDetector;
        private Mat image;

        public DetectWorker(SceneDetector det, byte[] data)
        {
            sceneDetector = det;
            image = new Mat(1080 + 1080/2, 1920, CvType.CV_8UC1);
            image.put(0, 0, data);
            Imgproc.cvtColor(image, image, Imgproc.COLOR_YUV2RGB_YV12);
        }

        public void run()
        {
            //Log.d(TAG, String.format("Detecting scene [%d]", getId()));

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

            sceneDetector.busy = false;
        }

    };

}