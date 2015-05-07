package com.babat.sandbox;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;

import android.app.Activity;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.babat.sandbox.graphics.MainGLSurfaceView;
import com.babat.sandbox.graphics.Vector3D;

public class MainActivity extends Activity implements CameraView.CameraViewListener {

    protected static final String TAG = "SandBox";

    private MainGLSurfaceView mGraphicsView;
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
        mGraphicsView = (MainGLSurfaceView) findViewById(R.id.GraphicsView);

        mCameraView.setZOrderOnTop(false);
        mGraphicsView.setZOrderOnTop(true);

//        mCameraView.setVisibility(View.GONE);

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


    public void onCameraFrame(byte[] data)
    {
        positionDetector.fix();
        sceneDetector.compute(data);
    }


    //RENDERING
    void render(Mat rodr, Mat rvec, Mat tvec, MatOfPoint2f axisPoints)
    {
        Mat rtrans = new Mat();
        Mat cam = new Mat();

        logMatrix("axis points", axisPoints);


        Core.transpose(rodr, rtrans);

        Core.gemm(rtrans, tvec, 1, tvec, 0, cam);

        double[] buf = new double[1];

        Mat view = Mat.zeros(4, 4, CvType.CV_64F);
        for (int row=0; row<3; row++) {
            for (int col=0; col<3; col++) {
                buf[0] = rodr.get(row, col)[0];
                view.put(row, col, buf);
            }

            buf[0] = tvec.get(row, 0)[0];
            view.put(row, 3, buf);
        }

        buf[0] = 1;
        view.put(3, 3, buf);

        //logMatrix("Initial cv view", view);

        Mat inv = Mat.zeros(4, 4, CvType.CV_64F);

        buf[0] = 1;
        inv.put(0, 0, buf);
        inv.put(3, 3, buf);

        buf[0] = -1;
        inv.put(1, 1, buf);
        inv.put(2, 2, buf);

        Mat viewNew = Mat.zeros(4, 4, CvType.CV_64F);
        Core.gemm(inv, view, 1, tvec, 0, viewNew);

        //logMatrix("Axis inverted view", viewNew);

        for (int col=0; col<4; col++) {
            for (int row=0; row<4; row++) {
                mGraphicsView.mRenderer.mCamera.mView[col*4+row] = (float) viewNew.get(row, col)[0];
            }
        }

        float[] inverseView = new float[16];
        Matrix.invertM(inverseView, 0, mGraphicsView.mRenderer.mCamera.mView, 0);

        float[] eyeInViewSpace = {0.0f, 0.0f, 0.0f, 1.0f};
        float[] lookAtInViewSpace = {0.0f, 0.0f, -1.0f, 1.0f};
        float[] upInViewSpace = {0.0f, 1.0f, 0.0f, 1.0f};

        float[] eyeInWorldSpace = new float[4];
        float[] lookAtInWorldSpace = new float[4];
        float[] upInWorldSpace = new float[4];

        Matrix.multiplyMV(eyeInWorldSpace, 0, inverseView, 0, eyeInViewSpace, 0);
        Matrix.multiplyMV(lookAtInWorldSpace, 0, inverseView, 0, lookAtInViewSpace, 0);
        Matrix.multiplyMV(upInWorldSpace, 0, inverseView, 0, upInViewSpace, 0);

        Log.d("Correct eye:", String.format("%f %f %f", eyeInWorldSpace[0], eyeInWorldSpace[1], eyeInWorldSpace[2]));
        Log.d("Correct lookAt:", String.format("%f %f %f", lookAtInWorldSpace[0], lookAtInWorldSpace[1], lookAtInWorldSpace[2]));

        Mat viewTrans = Mat.zeros(4, 4, CvType.CV_64F);
        Core.transpose(viewNew, viewTrans);

        //logMatrix("Transposed view", viewTrans);

        Mat viewInv = Mat.zeros(4, 4, CvType.CV_64F);
        Core.invert(viewTrans, viewInv);

        //logMatrix("Inverted view", viewInv);

        Vector3D eye = new Vector3D(eyeInWorldSpace[0], eyeInWorldSpace[1], eyeInWorldSpace[2]);
        Vector3D lookAt = new Vector3D(lookAtInWorldSpace[0], lookAtInWorldSpace[1], lookAtInWorldSpace[2]);
        Vector3D up = new Vector3D(upInWorldSpace[0], upInWorldSpace[1], upInWorldSpace[2]);

        mGraphicsView.startRendering(eye, lookAt, up);
    }

    public void logMatrix(String s, Mat m) {
        for (int rIdx = 0; rIdx < m.rows(); rIdx++) {
            for (int cIdx = 0; cIdx < m.cols(); cIdx++) {
                double[] val = m.get(rIdx, cIdx);
                Log.d(TAG, String.format(s+" [%d %d] = %f %f", rIdx, cIdx, val[0], val[1]));
            }
        }
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status)
        {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.d(TAG, "OpenCV loaded successfully");
                    sceneDetector = SceneDetector.getInstance(this.mAppContext);
                    mCameraView.addCameraViewListener((CameraView.CameraViewListener) this.mAppContext);
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

}
