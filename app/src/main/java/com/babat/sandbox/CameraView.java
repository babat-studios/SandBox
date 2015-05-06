package com.babat.sandbox;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;



public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback{

    private static final String TAG = "TestCamera";

    private SurfaceHolder surfaceHolder;
    private CameraViewListener cameraViewListener;
    private Camera camera;


    public CameraView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    public void enableView()
    {
        try {
            camera = Camera.open();
        } catch (RuntimeException e) {
            e.printStackTrace();
            return;
        }

        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(1920, 1080);
        parameters.setPreviewFpsRange(24000, 24000);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        camera.setParameters(parameters);

        Log.d(TAG, camera.getParameters().flatten());

        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.setPreviewCallback(this);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void disableView()
    {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }


    public void surfaceCreated(SurfaceHolder holder)
    {
    }


    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }


    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        if (surfaceHolder.getSurface() == null) {
            return;
        }

        try {
            camera.stopPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void onPreviewFrame(byte[] data, Camera camera)
    {
        if (cameraViewListener != null) {
            cameraViewListener.onCameraFrame(data);
        }
    }


    public void addCameraViewListener(CameraViewListener listener)
    {
        cameraViewListener = listener;
    }


    interface CameraViewListener
    {
        public void onCameraFrame(byte[] data);
    }
    
}
