package com.babat.vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.hardware.Camera.Area;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;


public class CameraView extends JavaCameraView {

    private static final String TAG = "MyView";

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @SuppressWarnings("deprecation")
    public void testMethod() {
        List<Area> arlist = new ArrayList<Area> ();
     
        arlist.add( new Area(new Rect(0, 0, 50, 50), 1 ) );
        mCamera.getParameters().setFocusAreas(arlist);
    }
    
}
