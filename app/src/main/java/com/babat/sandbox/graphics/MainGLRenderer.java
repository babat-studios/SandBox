package com.babat.sandbox.graphics;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import org.opencv.core.Mat;

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

    private Gnomon mGnomon = new Gnomon();
    private Cube mCube = new Cube();
    public Camera mCamera = new Camera();

    public float[] mView = new float[16];
    public float[] mProj = new float[16];

    private int mGnomonShader;
    private int mWorldShader;

    public boolean mIsCalibrated = true;

    public MainGLRenderer(Context context) { mContext = context; }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, "world.vert");
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, "world.frag");

        mWorldShader = GLES20.glCreateProgram();
        GLES20.glAttachShader(mWorldShader, vertexShader);
        GLES20.glAttachShader(mWorldShader, fragmentShader);
        GLES20.glLinkProgram(mWorldShader);

        Log.d("World Shader:", String.valueOf(GLES20.glGetShaderInfoLog(vertexShader)));
        Log.d("World Shader:", String.valueOf(GLES20.glGetShaderInfoLog(fragmentShader)));
        Log.d("World Shader:", String.valueOf(GLES20.glGetProgramInfoLog(mWorldShader)));

        vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, "gnomon.vert");
        fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, "gnomon.frag");

        mGnomonShader = GLES20.glCreateProgram();
        GLES20.glAttachShader(mGnomonShader, vertexShader);
        GLES20.glAttachShader(mGnomonShader, fragmentShader);
        GLES20.glLinkProgram(mGnomonShader);

        Log.d("Gnomon Shader:", String.valueOf(GLES20.glGetShaderInfoLog(vertexShader)));
        Log.d("Gnomon Shader:", String.valueOf(GLES20.glGetShaderInfoLog(fragmentShader)));
        Log.d("Gnomon Shader:", String.valueOf(GLES20.glGetProgramInfoLog(mGnomonShader)));

        mCamera.eye = new Vector3D(5.0f, 5.0f, 5.0f);
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

        if (!mIsCalibrated)
            return;

        mCamera.view(mView);
        mCamera.perspective(mProj);

        GLES20.glUseProgram(mWorldShader);
        mCube.draw(this);

        GLES20.glUseProgram(mGnomonShader);
        float[] viewProj = new float[16];
        Matrix.multiplyMM(viewProj, 0, mProj, 0, mView, 0);
//        Matrix.setIdentityM(viewProj, 0);
//        mGnomon.draw(mGnomonShader, mProj);
        mGnomon.draw(mGnomonShader, viewProj);
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

    public int getProgram() { return mWorldShader; }
}