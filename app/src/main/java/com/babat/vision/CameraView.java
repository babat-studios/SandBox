package com.babat.vision;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;


public class CameraView extends JavaCameraView {

    private static final String TAG = "TestCamera";

    public CameraView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }


//    public void surfaceChanged(SurfaceHolder holder, int format, int width,
//                               int height) {
//        Log.d(TAG, "SURFACE CHANGED");
//    }

    @SuppressWarnings("deprecation")
    public void setUpCamera()
    {
        mCamera.stopPreview();

        Camera.Parameters parameters = mCamera.getParameters();

        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        parameters.setPreviewSize(previewSizes.get(0).width, previewSizes.get(0).height); //Assuming the first one is best
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        mCamera.setParameters(parameters);

        try {
            mCamera.setPreviewDisplay(getHolder());
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, String.format(mCamera.getParameters().flatten()));
    }
    
}
