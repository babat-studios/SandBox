package com.babat.sandbox.graphics;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by Azad on 07/05/2015.
 */
public class Mesh {
    private static final String TAG = Mesh.class.toString();

    private final int mShaderProgram;

    private Vector3D mTranslation;
    private Vector3D mRotation;
    private Vector3D mScale;

    private float[] mAmbientColor = {0, 0, 0};
    private float[] mDiffuseColor = {0, 0, 0};
    private float[] mSpecularColor = {0, 0, 0};
    private float mShininess = 0;

    private float[] mVertices;
    private float[] mUVs;
    private float[] mNormals;

    private int[] mFaces;
    private int mIteration = 0;

    public Mesh(int shaderProgram) {
        mShaderProgram = shaderProgram;

        mTranslation = new Vector3D(0, 0, 0);
        mRotation = new Vector3D(0, 0, 0);
        mScale = new Vector3D(1, 1, 1);

        mAmbientColor = new float[4];
        mAmbientColor[0] = 0.0f;
        mAmbientColor[1] = 0.0f;
        mAmbientColor[2] = 0.0f;
        mAmbientColor[3] = 1.0f;

        mDiffuseColor = new float[4];
        mDiffuseColor[0] = 0.7f;
        mDiffuseColor[1] = 1.0f;
        mDiffuseColor[2] = 0.7f;
        mDiffuseColor[3] = 1.0f;

        mSpecularColor = new float[4];
        mSpecularColor[0] = 0.4f;
        mSpecularColor[1] = 0.4f;
        mSpecularColor[2] = 0.4f;
        mSpecularColor[3] = 1.4f;

        mShininess = 0.4f * 128;

        initToCube();
    }

    private void initToCube() {
        float[] v = {
            1.0f  , -1.0f , -1.0f,
            1.0f  , -1.0f ,  1.0f,
            -1.0f , -1.0f ,  1.0f,
            -1.0f , -1.0f , -1.0f,
            1.0f  , 1.0f  , -1.0f,
            1.0f  , 1.0f  , 1.0f,
            -1.0f , 1.0f  , 1.0f,
            -1.0f , 1.0f  , -1.0f
        };

        float[] vt = {
        };

        float[] vn = {
            0.0f  , -1.0f , 0.0f,
            0.0f  , 1.0f  , 0.0f,
            1.0f  , 0.0f  , 0.0f,
            0.0f  , 0.0f  , 1.0f,
            -1.0f , 0.0f  , 0.0f,
            0.0f  , 0.0f  , -1.0f,
        };

        int[] f = {
            2,0,1,  3,0,1,  4,0,1,
            8,0,2,  7,0,2,  6,0,2,
            5,0,3,  6,0,3,  2,0,3,
            6,0,4,  7,0,4,  3,0,4,
            3,0,5,  7,0,5,  8,0,5,
            1,0,6,  4,0,6,  8,0,6,
            1,0,1,  2,0,1,  4,0,1,
            5,0,2,  8,0,2,  6,0,2,
            1,0,3,  5,0,3,  2,0,3,
            2,0,4,  6,0,4,  3,0,4,
            4,0,5,  3,0,5,  8,0,5,
            5,0,6,  1,0,6,  8,0,6,
        };

        initFromObjFile(v, vt, vn, f);
    }

    private void initFromObjFile(float[] v, float[] vt, float[] vn, int[] f) {
        mVertices = new float[v.length];
        System.arraycopy(v, 0, mVertices, 0, v.length);

        mUVs = new float[vt.length];
        System.arraycopy(vt, 0, mUVs, 0, vt.length);

        mNormals = new float[vn.length];
        System.arraycopy(vn, 0, mNormals, 0, vn.length);

        mFaces = new int[f.length];
        System.arraycopy(f, 0, mFaces, 0, f.length);
    }

    public void draw(float[] parentTransform, float[] viewTransform, float[] projectionTransform, Vector3D eyePos, Vector3D lightPos, Vector3D lightColor) {
        /*
            Creating attribute buffers
         */
        float[] vertexArray = new float[mFaces.length * Utils.COORDS_PER_VERTEX];
        float[] normalArray = new float[mFaces.length * Utils.COORDS_PER_NORMAL];

        for (int i=0; i<mFaces.length; i+=3) {
            int vertexNo = mFaces[i  ] - 1;
            int uvNo     = mFaces[i+1] - 1;
            int normalNo = mFaces[i+2] - 1;

            System.arraycopy(mVertices, vertexNo*Utils.COORDS_PER_VERTEX, vertexArray, (i/3)*Utils.COORDS_PER_VERTEX, Utils.COORDS_PER_VERTEX);
            System.arraycopy(mNormals, normalNo*Utils.COORDS_PER_NORMAL, normalArray, (i/3)*Utils.COORDS_PER_NORMAL, Utils.COORDS_PER_NORMAL);
        }

        /*
            Debug blet
         */
//        Log.d(TAG, "vertices:");
//        for (int i=0; i<vertexArray.length; i+=3) {
//            Log.d(TAG, String.format("%f , %f , %f", vertexArray[i], vertexArray[i+1], vertexArray[i+2]));
//        }
//
//        Log.d(TAG, "normals:");
//        for (int i=0; i<normalArray.length; i+=3) {
//            Log.d(TAG, String.format("%f , %f , %f", normalArray[i], normalArray[i+1], normalArray[i+2]));
//        }
//
        FloatBuffer vertexBuffer = Utils.createFloatBuffer(vertexArray);
        FloatBuffer normalBuffer = Utils.createFloatBuffer(normalArray);

        /*
            Setting transformations
         */
        float[] modelTransform = new float[16];
        System.arraycopy(parentTransform, 0, modelTransform, 0, 16);
        Matrix.setIdentityM(modelTransform, 0);
        Matrix.translateM(modelTransform, 0, mTranslation.x, mTranslation.y, mTranslation.z);
        Matrix.rotateM(modelTransform, 0, mRotation.x, 1, 0, 0);
        Matrix.rotateM(modelTransform, 0, mRotation.y, 0, 1, 0);
        Matrix.rotateM(modelTransform, 0, mRotation.z, 0, 0, 1);
        Matrix.scaleM(modelTransform, 0, mScale.x, mScale.y, mScale.z);

        float[] inverseTransform = new float[16];
        float[] normalTransform = new float[16];
        Matrix.invertM(inverseTransform, 0, modelTransform, 0);
        Matrix.transposeM(normalTransform, 0, inverseTransform, 0);

        /*
            Debug blet
         */
        if (mIteration == 0) {
            Utils.logMatrix(TAG, "projectionTransform", projectionTransform);
            Utils.logMatrix(TAG, "viewTransform", viewTransform);
            Utils.logMatrix(TAG, "modelTransform", modelTransform);
            Utils.logMatrix(TAG, "normalTransform", normalTransform);
            mIteration = 1;
        }

        /*
            Loading shader variables
         */
        int handle;

        /* Transformations */
        handle =  GLES20.glGetUniformLocation(mShaderProgram, "gModelMatrix");
        GLES20.glUniformMatrix4fv(handle, 1, false, modelTransform, 0);

        handle =  GLES20.glGetUniformLocation(mShaderProgram, "gViewMatrix");
        GLES20.glUniformMatrix4fv(handle, 1, false, viewTransform, 0);

        handle =  GLES20.glGetUniformLocation(mShaderProgram, "gProjectionMatrix");
        GLES20.glUniformMatrix4fv(handle, 1, false, projectionTransform, 0);

        handle =  GLES20.glGetUniformLocation(mShaderProgram, "gNormalMatrix");
        GLES20.glUniformMatrix4fv(handle, 1, false, normalTransform, 0);

        /* Material */
        handle = GLES20.glGetUniformLocation(mShaderProgram, "gAmbientColor");
        GLES20.glUniform4fv(handle, 1, mAmbientColor, 0);

        handle = GLES20.glGetUniformLocation(mShaderProgram, "gDiffuseColor");
        GLES20.glUniform4fv(handle, 1, mDiffuseColor, 0);

        handle = GLES20.glGetUniformLocation(mShaderProgram, "gSpecularColor");
        GLES20.glUniform4fv(handle, 1, mSpecularColor, 0);

        handle = GLES20.glGetUniformLocation(mShaderProgram, "gShininess");
        GLES20.glUniform1f(handle, mShininess);

        /* Vertex attributes */
        handle = GLES20.glGetAttribLocation(mShaderProgram, "gVertexPosition");
        GLES20.glEnableVertexAttribArray(handle);
        GLES20.glVertexAttribPointer(handle, Utils.COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, Utils.COORDS_PER_VERTEX * 4, vertexBuffer);

        handle = GLES20.glGetAttribLocation(mShaderProgram, "gVertexNormal");
        GLES20.glEnableVertexAttribArray(handle);
        GLES20.glVertexAttribPointer(handle, Utils.COORDS_PER_NORMAL, GLES20.GL_FLOAT, false, Utils.COORDS_PER_NORMAL * 4, normalBuffer);

        /* Lighting */
        handle = GLES20.glGetUniformLocation(mShaderProgram, "gEyePosition");
        GLES20.glUniform4f(handle, eyePos.x, eyePos.y, eyePos.z, 1.0f);

        handle = GLES20.glGetUniformLocation(mShaderProgram, "gLightPosition");
        GLES20.glUniform4f(handle, lightPos.x, lightPos.y, lightPos.z, 1.0f);

        handle = GLES20.glGetUniformLocation(mShaderProgram, "gLightColor");
        GLES20.glUniform4f(handle, lightColor.x, lightColor.y, lightColor.z, 1.0f);

        /*
            Almighty renderer
         */
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mFaces.length / 3);
    }
}
