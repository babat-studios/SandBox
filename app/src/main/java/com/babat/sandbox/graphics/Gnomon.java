package com.babat.sandbox.graphics;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by Azad on 07/05/2015.
 */
public class Gnomon {
    private float mLineData[] = {
        //  X     Y     Z
        0.0f, 0.0f, 0.0f,
        2.0f, 0.0f, 0.0f,
        // ----------- //
        0.0f, 0.0f, 0.0f,
        0.0f, 2.0f, 0.0f,
        // ----------- //
        0.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 2.0f,
    };

    private float mColorData[] = {
        //  R     G     B
        1.0f, 0.0f, 0.0f,
        1.0f, 0.0f, 0.0f,
        // ----------- //
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        // ----------- //
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
    };

    public void draw(int shaderProgram, float[] transform) {
        float[] translation = new float[16];
        Matrix.setIdentityM(translation, 0);
//        Matrix.translateM(translation, 0, 0, 0, -5);

        float[] finalTransform = new float[16];
        Matrix.multiplyMM(finalTransform, 0, transform, 0, translation, 0);

        ByteBuffer bb = ByteBuffer.allocateDirect(6 * 3 * 4);
        bb.order(ByteOrder.nativeOrder());

        FloatBuffer vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(mLineData);
        vertexBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(6 * 3 * 4);
        bb2.order(ByteOrder.nativeOrder());

        FloatBuffer colorBuffer = bb2.asFloatBuffer();
        colorBuffer.put(mColorData);
        colorBuffer.position(0);

        int transformHandle = GLES20.glGetUniformLocation(shaderProgram, "transform");
        GLES20.glUniformMatrix4fv(transformHandle, 1, false, finalTransform, 0);

        int vertexHandle = GLES20.glGetAttribLocation(shaderProgram, "vertex");
        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 3 * 4, vertexBuffer);

        int colorHandle = GLES20.glGetAttribLocation(shaderProgram, "color");
        GLES20.glEnableVertexAttribArray(colorHandle);
        GLES20.glVertexAttribPointer(colorHandle, 3, GLES20.GL_FLOAT, false, 3 * 4, colorBuffer);

        GLES20.glLineWidth(3);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 6);
        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glLineWidth(1);
    }
}
