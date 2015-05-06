package com.babat.sandbox.graphics;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by Azad on 01/05/2015.
 */
public class Cube {
    private float mTriangleData[] = {
        //  X     Y     Z
        -1.0f, -1.0f, -1.0f,
        1.0f, -1.0f, -1.0f,
        -1.0f, 1.0f, -1.0f,

        -1.0f, 1.0f, -1.0f,
        1.0f, -1.0f, -1.0f,
        1.0f, 1.0f, -1.0f,
        // ----------- //
        -1.0f, 1.0f, -1.0f,
        1.0f, 1.0f, -1.0f,
        -1.0f, 1.0f, 1.0f,

        -1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, -1.0f,
        1.0f, 1.0f, 1.0f,
        // ----------- //
        1.0f, 1.0f, 1.0f,
        -1.0f, 1.0f, 1.0f,
        -1.0f, -1.0f, 1.0f,

        1.0f, 1.0f, 1.0f,
        -1.0f, -1.0f, 1.0f,
        1.0f, -1.0f, 1.0f,
        // ----------- //
        1.0f, -1.0f, 1.0f,
        -1.0f, -1.0f, 1.0f,
        -1.0f, -1.0f, -1.0f,

        1.0f, -1.0f, 1.0f,
        -1.0f, -1.0f, -1.0f,
        1.0f, -1.0f, -1.0f,
        // ----------- //
        1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, -1.0f,
        1.0f, -1.0f, -1.0f,

        1.0f, 1.0f, 1.0f,
        1.0f, -1.0f, -1.0f,
        1.0f, -1.0f, 1.0f,
        // ----------- //
        -1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f, 1.0f,
        -1.0f, 1.0f, -1.0f,

        -1.0f, 1.0f, -1.0f,
        -1.0f, -1.0f, 1.0f,
        -1.0f, 1.0f, 1.0f,
    };

    float mNormalData[] = {
//            //  X     Y     Z
//            -1.0f, -1.0f, -1.0f,
//            1.0f, -1.0f, -1.0f,
//            -1.0f, 1.0f, -1.0f,
//
//            -1.0f, 1.0f, -1.0f,
//            1.0f, -1.0f, -1.0f,
//            1.0f, 1.0f, -1.0f,
//            // ----------- //
//            -1.0f, 1.0f, -1.0f,
//            1.0f, 1.0f, -1.0f,
//            -1.0f, 1.0f, 1.0f,
//
//            -1.0f, 1.0f, 1.0f,
//            1.0f, 1.0f, -1.0f,
//            1.0f, 1.0f, 1.0f,
//            // ----------- //
//            1.0f, 1.0f, 1.0f,
//            -1.0f, 1.0f, 1.0f,
//            -1.0f, -1.0f, 1.0f,
//
//            1.0f, 1.0f, 1.0f,
//            -1.0f, -1.0f, 1.0f,
//            1.0f, -1.0f, 1.0f,
//            // ----------- //
//            1.0f, -1.0f, 1.0f,
//            -1.0f, -1.0f, 1.0f,
//            -1.0f, -1.0f, -1.0f,
//
//            1.0f, -1.0f, 1.0f,
//            -1.0f, -1.0f, -1.0f,
//            1.0f, -1.0f, -1.0f,
//            // ----------- //
//            1.0f, 1.0f, 1.0f,
//            1.0f, 1.0f, -1.0f,
//            1.0f, -1.0f, -1.0f,
//
//            1.0f, 1.0f, 1.0f,
//            1.0f, -1.0f, -1.0f,
//            1.0f, -1.0f, 1.0f,
//            // ----------- //
//            -1.0f, -1.0f, -1.0f,
//            -1.0f, -1.0f, 1.0f,
//            -1.0f, 1.0f, -1.0f,
//
//            -1.0f, 1.0f, -1.0f,
//            -1.0f, -1.0f, 1.0f,
//            -1.0f, 1.0f, 1.0f,
        0.0f, 0.0f, -1.0f,
        0.0f, 0.0f, -1.0f,
        0.0f, 0.0f, -1.0f,
        0.0f, 0.0f, -1.0f,
        0.0f, 0.0f, -1.0f,
        0.0f, 0.0f, -1.0f,

        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,

        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,

        0.0f, -1.0f, 0.0f,
        0.0f, -1.0f, 0.0f,
        0.0f, -1.0f, 0.0f,
        0.0f, -1.0f, 0.0f,
        0.0f, -1.0f, 0.0f,
        0.0f, -1.0f, 0.0f,

        1.0f, 0.0f, 0.0f,
        1.0f, 0.0f, 0.0f,
        1.0f, 0.0f, 0.0f,
        1.0f, 0.0f, 0.0f,
        1.0f, 0.0f, 0.0f,
        1.0f, 0.0f, 0.0f,

        -1.0f, 0.0f, 0.0f,
        -1.0f, 0.0f, 0.0f,
        -1.0f, 0.0f, 0.0f,
        -1.0f, 0.0f, 0.0f,
        -1.0f, 0.0f, 0.0f,
        -1.0f, 0.0f, 0.0f
    };

    private int mCoordsPerVertex = 3;
    private int mVertexCount = mTriangleData.length / mCoordsPerVertex;

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mNormalBuffer;
    private float mAngle = 0;

    public void rotate(float angle) {
        mAngle += angle;

        while (mAngle >= 360) mAngle -= 360;
    }

    public void draw(MainGLRenderer renderer) {
        ByteBuffer bb = ByteBuffer.allocateDirect(mVertexCount * mCoordsPerVertex * 4);
        bb.order(ByteOrder.nativeOrder());

        mVertexBuffer = bb.asFloatBuffer();
        mVertexBuffer.put(mTriangleData);
        mVertexBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(mVertexCount * mCoordsPerVertex * 4);
        bb2.order(ByteOrder.nativeOrder());

        mNormalBuffer = bb2.asFloatBuffer();
        mNormalBuffer.put(mNormalData);
        mNormalBuffer.position(0);

        float[] rot = new float[16];
        Matrix.setIdentityM(rot, 0);
        Matrix.rotateM(rot, 0, mAngle, 0.0f, 0.0f, 1.0f);

        float[] vpMatrix = new float[16];
        Matrix.multiplyMM(vpMatrix, 0, renderer.mProj, 0, renderer.mView, 0);

        float[] mvpMatrix = new float[16];
        float[] inverseMatrix = new float[16];
        float[] normalMatrix = new float[16];
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, rot, 0);
        Matrix.invertM(inverseMatrix, 0, mvpMatrix, 0);
        Matrix.transposeM(normalMatrix, 0, inverseMatrix, 0);

        int vpHandle = GLES20.glGetUniformLocation(renderer.getProgram(), "gViewProjectionMatrix");
        GLES20.glUniformMatrix4fv(vpHandle, 1, false, vpMatrix, 0);

        int mHandle = GLES20.glGetUniformLocation(renderer.getProgram(), "gModelMatrix");
        GLES20.glUniformMatrix4fv(mHandle, 1, false, rot, 0);

        int nHandle = GLES20.glGetUniformLocation(renderer.getProgram(), "gNormalMatrix");
        GLES20.glUniformMatrix4fv(nHandle, 1, false, normalMatrix, 0);

        float lightPos[] = { 4.0f, 1.0f, 6.0f, 1.0f };
        int lightHandle = GLES20.glGetUniformLocation(renderer.getProgram(), "gLightPosition");
        GLES20.glUniform4fv(lightHandle, 1, lightPos, 0);

        int positionHandle = GLES20.glGetAttribLocation(renderer.getProgram(), "gVertexPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, mCoordsPerVertex, GLES20.GL_FLOAT, false, mCoordsPerVertex * 4, mVertexBuffer);

        int normalHandle = GLES20.glGetAttribLocation(renderer.getProgram(), "gVertexNormal");
        GLES20.glEnableVertexAttribArray(normalHandle);
        GLES20.glVertexAttribPointer(normalHandle, mCoordsPerVertex, GLES20.GL_FLOAT, false, mCoordsPerVertex * 4, mNormalBuffer);

        float color[] = { 0.7f, 1.0f, 0.7f, 1.0f };
        int colorHandle = GLES20.glGetUniformLocation(renderer.getProgram(), "gDiffuseColor");
        GLES20.glUniform4fv(colorHandle, 1, color, 0);

        float ambient[] = { 0.3f, 0.3f, 0.3f, 1.0f };
        int ambientHandle = GLES20.glGetUniformLocation(renderer.getProgram(), "gAmbientColor");
        GLES20.glUniform4fv(ambientHandle, 1, ambient, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mVertexCount);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }
}
