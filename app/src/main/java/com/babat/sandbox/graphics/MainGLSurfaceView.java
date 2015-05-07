package com.babat.sandbox.graphics;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Created by Azad on 30/04/2015.
 */
public class MainGLSurfaceView extends GLSurfaceView {

    public final MainGLRenderer mRenderer;

    public MainGLSurfaceView(Context context, AttributeSet attrs){
        super(context, attrs);

        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);

        mRenderer = new MainGLRenderer(this.getContext());
        setRenderer(mRenderer);
    }

    public void startRendering(Vector3D eye, Vector3D lookAt, Vector3D up) {
        mRenderer.mIsCalibrated = true;
        mRenderer.mCamera.eye = eye;
        mRenderer.mCamera.lookAt = lookAt;
        mRenderer.mCamera.up = up;

        Log.d("CALIB", String.format("eye    = %f %f %f", eye.x, eye.y, eye.z));
        Log.d("CALIB", String.format("lookAt = %f %f %f", lookAt.x, lookAt.y, lookAt.z));
    }
}