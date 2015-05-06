package com.babat.sandbox.graphics;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Azad on 30/04/2015.
 */
public class MainGLRenderer implements GLSurfaceView.Renderer {
    private Context mContext;

    private Cube mCube = new Cube();
    public Camera mCamera = new Camera();

    public float[] mView = new float[16];
    public float[] mProj = new float[16];

    private int mProgram;
    public boolean mIsCalibrated = false;

    public MainGLRenderer(Context context) { mContext = context; }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, "shader.vert");
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, "shader.frag");

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        Log.d("Shader:", String.valueOf(GLES20.glGetShaderInfoLog(vertexShader)));
        Log.d("Shader:", String.valueOf(GLES20.glGetShaderInfoLog(fragmentShader)));
        Log.d("Shader:", String.valueOf(GLES20.glGetProgramInfoLog(mProgram)));

        mCamera.eye = new Vector3D(7.5f, -6.5f, 5.35f);
        mCamera.lookAt = new Vector3D(0.0f, 0.0f, 0.0f);
        mCamera.up = new Vector3D(0.0f, 0.0f, 0.1f);
        mCamera.fov = 60;
        mCamera.aspect = 1;

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
//                mCube.rotate(5);
            }
        }, 0, 50);
    }

    public void onDrawFrame(GL10 unused) {
        GLES20.glClearDepthf(1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);

        if (mIsCalibrated) {
            mCamera.view(mView);
            mCamera.perspective(mProj);
            mCube.draw(this);
        }
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mCamera.aspect = (1.0f*width)/height;
    }

    public int loadShader(int type, String shaderFileName) {
        int shader = -1;
        try {
            InputStream f = mContext.getAssets().open(shaderFileName);
            BufferedReader in = new BufferedReader(new InputStreamReader(f));

            StringBuilder buf = new StringBuilder();
            while (true) {
                String line = in.readLine();
                if (line == null) break;
                buf.append(line);
                buf.append('\n');
            }

            in.close();

            String shaderCode = buf.toString();
            shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
        }
        catch (IOException e) {
            Log.e("", "Error reading " + shaderFileName);
        }

        return shader;
    }

    public int getProgram() { return mProgram; }
}