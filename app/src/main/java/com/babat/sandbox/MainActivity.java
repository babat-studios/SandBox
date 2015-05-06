package com.babat.sandbox;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.babat.sandbox.graphics.MainGLSurfaceView;
import com.babat.sandbox.graphics.Vector3D;

public class MainActivity extends Activity {

    protected static final String TAG = "SandBox";

    private MainGLSurfaceView mGraphicsView;
    private CameraView mCameraView;

    private SceneDetector sceneDetector;


    //ACTIVITY

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mCameraView = (CameraView) findViewById(R.id.CameraView);
        mGraphicsView = (MainGLSurfaceView) findViewById(R.id.GraphicsView);

        mCameraView.setZOrderOnTop(false);
        mGraphicsView.setZOrderOnTop(true);

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }


    @Override
    public void onResume()
    {
        super.onResume();
        if (mCameraView != null) {
            mCameraView.enableView();
        }
    }


    @Override
    public void onPause()
    {
        super.onPause();
        if (mCameraView != null) {
            mCameraView.disableView();
        }
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mCameraView != null) {
            mCameraView.disableView();
        }
    }


    //RENDERING
    void render(Mat rodr, Mat rvec, Mat tvec)
    {
        Mat rtrans = new Mat();
        Mat cam = new Mat();

        Core.transpose(rodr, rtrans);

        Vector3D rotation = new Vector3D(
            (float) rvec.get(0, 0)[0],
            (float) rvec.get(1, 0)[0],
            (float) rvec.get(2, 0)[0]
        );

        Core.gemm(rtrans, tvec, 1, tvec, 0, cam);

        Vector3D translation = new Vector3D(
            (float) cam.get(0, 0)[0],
            (float) cam.get(1, 0)[0],
            (float) cam.get(2, 0)[0]
        );

        mGraphicsView.startRendering(rotation, translation);
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
