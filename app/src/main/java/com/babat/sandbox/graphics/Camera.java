package com.babat.sandbox.graphics;

import android.opengl.Matrix;

/**
 * Created by Azad on 30/04/2015.
 */
public class Camera {
    public Vector3D eye;
    public Vector3D lookAt;
    public Vector3D up;
    public float fov;
    public float aspect;

    public void view(float[] m) {
        Matrix.setLookAtM(m, 0, eye.x, eye.y, eye.z, lookAt.x, lookAt.y, lookAt.z, up.x, up.y, up.z);
    }

    public void perspective(float[] m) {
        Matrix.perspectiveM(m, 0, fov, aspect, 0.5f, 30f);
    }
}
