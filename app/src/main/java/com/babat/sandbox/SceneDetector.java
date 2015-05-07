package com.babat.sandbox;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfDouble;
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


public class SceneDetector {

    protected static final String TAG = "SceneDetector";

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

    public synchronized void compute(byte[] data) {
        if (!detected && !busy) {
            busy = true;
            DetectWorker dWorker = new DetectWorker(data);
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


    private class DetectWorker extends Thread {

        private Mat image;

        public DetectWorker(byte[] data)
        {
            image = new Mat(1080 + 1080/2, 1920, CvType.CV_8UC1);
            image.put(0, 0, data);
            Imgproc.cvtColor(image, image, Imgproc.COLOR_YUV2RGB_YV12);
        }

        public void run()
        {
            Log.d(TAG, String.format("Detecting scene [%d]", getId()));

            MatOfKeyPoint keypoints = new MatOfKeyPoint();
            detector.detect(image, keypoints);

            Mat descriptors  = new Mat();
            extractor.compute(image, keypoints, descriptors);

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
//                    detected = true;
                    calibrate(image, resList);
                }
            }

            busy = false;
        }

        private void calibrate(Mat img, List<Point> crosses)
        {
            Log.d(TAG, "Calibrating camera");


            //Input image size
            Size imgSize = img.size();

            //Zero distortion coefs
            MatOfDouble distCoefs = new MatOfDouble();

            //Nexus 5 camera view matrix
            Mat cameraMatrix = new Mat(3,3,5);
            //Estimation:
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


            //Corner recognition
            List<Double> xs = new ArrayList<Double>();
            List<Double> ys = new ArrayList<Double>();
            for (Point pnt : crosses) {
                xs.add(pnt.x);
                ys.add(pnt.y);
            }
            Collections.sort(xs);
            Collections.sort(ys);

            Point3[] objectPnts = new Point3[4];
            Point[] imagePnts = new Point[4];
            for (Point pnt : crosses) {
                int xIdx = xs.indexOf(pnt.x);
                int yIdx = ys.indexOf(pnt.y);

                int mIdx; //clockwise
                if (xIdx < 2 && yIdx < 2) { //top left
                    mIdx = 3;
                    objectPnts[mIdx] = new Point3(-1, 1, 0);
                } else if (xIdx >= 2 && yIdx < 2) { //top right
                    mIdx = 0;
                    objectPnts[mIdx] = new Point3(1, 1, 0);
                } else if (xIdx >= 2 && yIdx >= 2) { //bottom right
                    mIdx = 1;
                    objectPnts[mIdx] = new Point3(1, -1, 0);
                } else { //bottom left
                    mIdx = 2;
                    objectPnts[mIdx] = new Point3(-1, -1, 0);
                }
                imagePnts[mIdx] = new Point(pnt.x, pnt.y);
            }

            //Debug
            for (int tIdx = 0; tIdx < 4; tIdx++) {
                Log.d(TAG,  String.format("Corner %d :: [%f %f] => [%f %f]", tIdx,
                        objectPnts[tIdx].x,
                        objectPnts[tIdx].y,
                        imagePnts[tIdx].x,
                        imagePnts[tIdx].y));
            }

            MatOfPoint3f objectPointView = new MatOfPoint3f();
            objectPointView.fromArray(objectPnts);
            MatOfPoint2f imagePointView = new MatOfPoint2f();
            imagePointView.fromArray(imagePnts);





            //Output arrays
            Mat rvec = new Mat();
            Mat tvec = new Mat();
            Calib3d.solvePnP(objectPointView, imagePointView, cameraMatrix, distCoefs, rvec, tvec);

            Mat rodr = new Mat();
            Calib3d.Rodrigues(rvec, rodr);


            //Debug
            for (int rIdx = 0; rIdx < cameraMatrix.rows(); rIdx++) {
                for (int cIdx = 0; cIdx < cameraMatrix.cols(); cIdx++) {
                    double[] val = cameraMatrix.get(rIdx, cIdx);
                    Log.d(TAG, String.format("Camera view matrix [%d %d] = %f", rIdx, cIdx, val[0]));
                }
            }
            for (int rIdx = 0; rIdx < tvec.rows(); rIdx++) {
                for (int cIdx = 0; cIdx < tvec.cols(); cIdx++) {
                    double[] val = tvec.get(rIdx, cIdx);
                    Log.d(TAG, String.format("tvec matrix [%d %d] = %f", rIdx, cIdx, val[0]));
                }
            }
            for (int rIdx = 0; rIdx < rodr.rows(); rIdx++) {
                for (int cIdx = 0; cIdx < rodr.cols(); cIdx++) {
                    double[] val = rodr.get(rIdx, cIdx);
                    Log.d(TAG, String.format("Rodrigues matrix [%d %d] = %f", rIdx, cIdx, val[0]));
                }
            }

            MatOfPoint3f axisReal = new MatOfPoint3f(
                    new Point3(0, 0, 0),
                    new Point3(2, 0, 0),
                    new Point3(0, 2, 0),
                    new Point3(0, 0, 2));
            MatOfPoint2f axis = new MatOfPoint2f();

            Calib3d.projectPoints(axisReal, rvec, tvec, cameraMatrix, distCoefs, axis);


            //Render the box
            context.render(rodr, rvec, tvec, axis);
        }

    };

}