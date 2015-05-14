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

    private Mat rvec;
    private Mat tvec;

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

    public native String test(byte[] data, int width, int height);

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

        private Mat image;
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

                Log.d(TAG, test(_data, 1920, 1080));

                image = new Mat(1080 + 1080 / 2, 1920, CvType.CV_8UC1);

                busy = true;
                image.put(0, 0, _data);
                busy = false;

                Imgproc.cvtColor(image, image, Imgproc.COLOR_YUV2RGB_YV12);

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
                        if (dm.distance < 50) { //hardcoded bf 50
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
                        if (crosses.get(pnt) > 2) { //hardcoded
                            resList.add(pnt);
                            Log.d(TAG, String.format("Cross found at (%f, %f) [%d]", pnt.x, pnt.y, getId()));
                        }
                    }

                    if (resList.size() == 4) {
                        calibrate(image, resList);
                    }
                }
            }
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

            MatOfPoint3f objectPointView = new MatOfPoint3f();
            objectPointView.fromArray(objectPnts);
            MatOfPoint2f imagePointView = new MatOfPoint2f();
            imagePointView.fromArray(imagePnts);

            //Output arrays
            rvec = new Mat();
            tvec = new Mat();
            Calib3d.solvePnP(objectPointView, imagePointView, cameraMatrix, distCoefs, rvec, tvec);
            detected = true;
        }

    };

}