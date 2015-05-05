package com.babat.vision;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;


public class MainActivity extends Activity implements CvCameraViewListener2 {

    protected static final String TAG = "MyVision";

    private CameraView mOpenCvCameraView;
    private SceneDetector sceneDetector;


    //ACTIVITY

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
 
        mOpenCvCameraView = (CameraView) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.d(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }


    //CAMERA VIEW

    public void onCameraViewStarted(int width, int height)
    {
        mOpenCvCameraView.setUpCamera();
        sceneDetector = SceneDetector.getInstance(this);
    }

    public void onCameraViewStopped()
    {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame)
    {
        Mat rgba = inputFrame.rgba();
        sceneDetector.compute(rgba);
        return rgba;
    }


    //RENDERING

    void render(Mat rvec, Mat tvec)
    {
        //RENDER!
    }


}
