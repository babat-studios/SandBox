package com.babat.sandbox;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;

/**
 * Created by aisfo on 07/05/15.
 */
public interface CameraPositionListener {

    void onCameraMoved(Mat rvec, Mat tvec);

}
