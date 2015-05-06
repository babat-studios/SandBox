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

    private final MainGLRenderer mRenderer;

    public MainGLSurfaceView(Context context, AttributeSet attrs){
        super(context, attrs);

        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);

        mRenderer = new MainGLRenderer(this.getContext());
        setRenderer(mRenderer);
    }

    public void startRendering(Vector3D rotation, Vector3D translation) {
        mRenderer.mIsCalibrated = true;
        mRenderer.mCamera.eye = translation;
        mRenderer.mCamera.lookAt = new Vector3D(0.0f, 0.0f, 0.0f);
        Log.d("CALIB", String.format("Eye = %f %f %f", translation.x, translation.y, translation.z));
    }
}