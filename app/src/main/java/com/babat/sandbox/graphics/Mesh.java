package com.babat.sandbox.graphics;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Azad on 07/05/2015.
 */
public class Mesh {
    private static final String TAG = Mesh.class.toString();

    private final int mShaderProgram;

    private Vector3D mTranslation;
    private Vector3D mRotation;
    private Vector3D mScale;

    private int[] mTexture;

    private float[] mVertices;
    private float[] mUVs;
    private float[] mNormals;

    private Material[] mMaterials;
    private GeometryObject[] mGeometries;

    public void rotate(int i) {
//        mRotation.z += 180;
        mScale = new Vector3D(0.5f, 0.5f, 0.5f);
    }

    public class GeometryObject {
        public String name;
        public int material;
        public int[] faces;

        public FloatBuffer vertexBuffer = null;
        public FloatBuffer uvBuffer = null;
        public FloatBuffer normalBuffer = null;
    }

    public class Material {
        String name;

        public float[] ka = {0.7f, 0.7f, 0.7f, 1.0f};
        public float[] kd = {0.7f, 0.7f, 0.7f, 1.0f};
        public float[] ks = {0.7f, 0.7f, 0.7f, 1.0f};

        public int map_ka;
        public int map_kd;
        public int map_ks;
    }

    public Mesh(int shaderProgram) {
        mShaderProgram = shaderProgram;

        mTranslation = new Vector3D(0, 0, 0);
        mRotation = new Vector3D(0, 0, 0);
        mScale = new Vector3D(1, 1, 1);

        loadFromJsonFile("mesh/batman.json");
    }

    private void loadFromJsonFile(String filename) {
        String jsonStr = new String();

        List<String> lines = Utils.readFile(filename);
        for (String line : lines) {
            jsonStr = jsonStr + line;
        }

        try {
            JSONObject json = new JSONObject(jsonStr);
            JSONArray jArr;

            jArr = json.getJSONArray("v");
            mVertices = new float[jArr.length()];
            for (int i=0; i<jArr.length(); i++) {
                mVertices[i] = (float) jArr.getDouble(i);
            }

            jArr = json.getJSONArray("vt");
            mUVs = new float[jArr.length()];
            for (int i=0; i<jArr.length(); i++) {
                mUVs[i] = (float) jArr.getDouble(i);
            }

            jArr = json.getJSONArray("vn");
            mNormals = new float[jArr.length()];
            for (int i=0; i<jArr.length(); i++) {
                mNormals[i] = (float) jArr.getDouble(i);
            }

            Map<String, Integer> matIndex = new HashMap<>();

            jArr = json.getJSONArray("materials");
            mMaterials = new Material[jArr.length()];
            for (int i=0; i<jArr.length(); i++) {
                JSONObject jMat = jArr.getJSONObject(i);
                JSONArray jComp;

                mMaterials[i] = new Material();
                mMaterials[i].name = jMat.getString("name");

                matIndex.put(mMaterials[i].name, i);

                jComp = jMat.getJSONArray("ka");
                mMaterials[i].ka[0] = (float) jComp.getDouble(0);
                mMaterials[i].ka[1] = (float) jComp.getDouble(1);
                mMaterials[i].ka[2] = (float) jComp.getDouble(2);

                jComp = jMat.getJSONArray("kd");
                mMaterials[i].kd[0] = (float) jComp.getDouble(0);
                mMaterials[i].kd[1] = (float) jComp.getDouble(1);
                mMaterials[i].kd[2] = (float) jComp.getDouble(2);

                jComp = jMat.getJSONArray("ks");
                mMaterials[i].ks[0] = (float) jComp.getDouble(0);
                mMaterials[i].ks[1] = (float) jComp.getDouble(1);
                mMaterials[i].ks[2] = (float) jComp.getDouble(2);
            }

            jArr = json.getJSONArray("objects");
            mGeometries = new GeometryObject[jArr.length()];
            for (int i=0; i<jArr.length(); i++) {
                JSONObject jMat = jArr.getJSONObject(i);
                JSONArray jFaces;

                mGeometries[i] = new GeometryObject();
                mGeometries[i].name = jMat.getString("name");

                String matName = jMat.getString("material");
                mGeometries[i].material = matIndex.get(matName);

                jFaces = jMat.getJSONArray("f");
                mGeometries[i].faces = new int[jFaces.length()];
                for (int j=0; j<jFaces.length(); j++) {
                    mGeometries[i].faces[j] = jFaces.getInt(j);
                }
            }

            Log.d(TAG, "Finished loading 3D object.");
        }
        catch (JSONException e) {
            Log.e(TAG, "Failed to load Json file.", e);
        }
    }

    public void draw(float[] parentTransform, float[] viewTransform, float[] projectionTransform, Vector3D eyePos, Vector3D lightPos, Vector3D lightColor) {
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
            Loading generic shader variables
         */
        int handle;

        /* Lighting */
        handle = GLES20.glGetUniformLocation(mShaderProgram, "gEyePosition");
        GLES20.glUniform4f(handle, eyePos.x, eyePos.y, eyePos.z, 1.0f);

        handle = GLES20.glGetUniformLocation(mShaderProgram, "gLightPosition");
        GLES20.glUniform4f(handle, lightPos.x, lightPos.y, lightPos.z, 1.0f);

        handle = GLES20.glGetUniformLocation(mShaderProgram, "gLightColor");
        GLES20.glUniform4f(handle, lightColor.x, lightColor.y, lightColor.z, 1.0f);

        /* Transformations */
        handle = GLES20.glGetUniformLocation(mShaderProgram, "gModelMatrix");
        GLES20.glUniformMatrix4fv(handle, 1, false, modelTransform, 0);

        handle = GLES20.glGetUniformLocation(mShaderProgram, "gViewMatrix");
        GLES20.glUniformMatrix4fv(handle, 1, false, viewTransform, 0);

        handle = GLES20.glGetUniformLocation(mShaderProgram, "gProjectionMatrix");
        GLES20.glUniformMatrix4fv(handle, 1, false, projectionTransform, 0);

        handle = GLES20.glGetUniformLocation(mShaderProgram, "gNormalMatrix");
        GLES20.glUniformMatrix4fv(handle, 1, false, normalTransform, 0);


        /*
            Drawing each object
         */
        for (GeometryObject geometry : mGeometries) {
            int faceCount = geometry.faces.length / 3;


            if (geometry.vertexBuffer == null) {
            /*
                Creating attribute buffers
             */
                float[] vertexArray = new float[faceCount * Utils.COORDS_PER_VERTEX];
                float[] uvArray = new float[faceCount * Utils.COORDS_PER_UV];
                float[] normalArray = new float[faceCount * Utils.COORDS_PER_NORMAL];

                float[] zeroes = {0.0f, 0.0f, 0.0f, 0.0f};

                for (int i = 0; i < geometry.faces.length; i += 3) {
                    int vertexNo = geometry.faces[i];
                    int uvNo = geometry.faces[i + 1];
                    int normalNo = geometry.faces[i + 2];

                    try {
                        System.arraycopy(mVertices, vertexNo * Utils.COORDS_PER_VERTEX, vertexArray, (i / 3) * Utils.COORDS_PER_VERTEX, Utils.COORDS_PER_VERTEX);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.arraycopy(zeroes, 0, vertexArray, (i / 3) * Utils.COORDS_PER_VERTEX, Utils.COORDS_PER_VERTEX);
                    }

                    try {
                        System.arraycopy(mUVs, uvNo * Utils.COORDS_PER_UV, uvArray, (i / 3) * Utils.COORDS_PER_UV, Utils.COORDS_PER_UV);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.arraycopy(zeroes, 0, uvArray, (i / 3) * Utils.COORDS_PER_UV, Utils.COORDS_PER_UV);
                    }

                    try {
                        System.arraycopy(mNormals, normalNo * Utils.COORDS_PER_NORMAL, normalArray, (i / 3) * Utils.COORDS_PER_NORMAL, Utils.COORDS_PER_NORMAL);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.arraycopy(zeroes, 0, normalArray, (i / 3) * Utils.COORDS_PER_NORMAL, Utils.COORDS_PER_NORMAL);
                    }
                }

                geometry.vertexBuffer = Utils.createFloatBuffer(vertexArray);
                geometry.uvBuffer = Utils.createFloatBuffer(uvArray);
                geometry.normalBuffer = Utils.createFloatBuffer(normalArray);
            }
            else if (geometry.uvBuffer == null || geometry.normalBuffer == null) {
                Log.e(TAG, "Some buffers are empty but not all.");
            }


            /*
                Loading object specific shader variables
             */

            /* Material */
            handle = GLES20.glGetUniformLocation(mShaderProgram, "gAmbientColor");
            GLES20.glUniform4fv(handle, 1, mMaterials[geometry.material].ka, 0);

            handle = GLES20.glGetUniformLocation(mShaderProgram, "gDiffuseColor");
            GLES20.glUniform4fv(handle, 1, mMaterials[geometry.material].kd, 0);

            handle = GLES20.glGetUniformLocation(mShaderProgram, "gSpecularColor");
            GLES20.glUniform4fv(handle, 1, mMaterials[geometry.material].ks, 0);

            handle = GLES20.glGetUniformLocation(mShaderProgram, "gShininess");
            GLES20.glUniform1f(handle, 0.0f);

            /* Vertex attributes */
            handle = GLES20.glGetAttribLocation(mShaderProgram, "gVertexPosition");
            GLES20.glEnableVertexAttribArray(handle);
            GLES20.glVertexAttribPointer(handle, Utils.COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, Utils.COORDS_PER_VERTEX * 4, geometry.vertexBuffer);

            handle = GLES20.glGetAttribLocation(mShaderProgram, "gVertexNormal");
            GLES20.glEnableVertexAttribArray(handle);
            GLES20.glVertexAttribPointer(handle, Utils.COORDS_PER_NORMAL, GLES20.GL_FLOAT, false, Utils.COORDS_PER_NORMAL * 4, geometry.normalBuffer);

            handle = GLES20.glGetAttribLocation(mShaderProgram, "gVertexUV");
            GLES20.glEnableVertexAttribArray(handle);
            GLES20.glVertexAttribPointer(handle, Utils.COORDS_PER_UV, GLES20.GL_FLOAT, false, Utils.COORDS_PER_UV * 4, geometry.uvBuffer);

            /*
                Almighty renderer
             */
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, faceCount);
        }
    }
}
