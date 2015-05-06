package com.babat.sandbox;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;


public class MainActivity extends Activity {

    protected static final String TAG = "SandBox";

    private CameraView mCameraView;
    private SceneDetector sceneDetector;
    private PositionDetector positionDetector;


    //ACTIVITY

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mCameraView = (CameraView) findViewById(R.id.CameraView);

        positionDetector = PositionDetector.getInstance(this);

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }


    @Override
    public void onResume()
    {
        super.onResume();
        if (mCameraView != null) {
            mCameraView.enableView();
        }
        positionDetector.enableDetector();
    }


    @Override
    public void onPause()
    {
        super.onPause();
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        positionDetector.disableDetector();
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        positionDetector.disableDetector();
    }


    //RENDERING
    void render(Mat rvec, Mat tvec)
    {
        //RENDER!
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status)
        {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.d(TAG, "OpenCV loaded successfully");
                    sceneDetector = SceneDetector.getInstance(this.mAppContext);
                    mCameraView.addCameraViewListener(sceneDetector);
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

}
