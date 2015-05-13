package com.babat.sandbox.graphics;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by Azad on 07/05/2015.
 */
public class Utils {
    public static final int VERTICES_PER_FACE = 3;
    public static final int COORDS_PER_VERTEX = 3;
    public static final int COORDS_PER_NORMAL = 3;

    public static FloatBuffer createFloatBuffer(float[] floatArray) {
        ByteBuffer bb = ByteBuffer.allocateDirect(floatArray.length * 4);
        bb.order(ByteOrder.nativeOrder());

        FloatBuffer result = bb.asFloatBuffer();
        result.put(floatArray);
        result.position(0);

        return result;
    }

    public static void logMatrix(String tag, String name, float[] matrix) {
        Log.d(tag, String.format(
                        "\n%s = [\n" +
                        "        %f, %f, %f, %f\n" +
                        "        %f, %f, %f, %f\n" +
                        "        %f, %f, %f, %f\n" +
                        "        %f, %f, %f, %f\n" +
                        "\n]",
                        name,
                        matrix[0], matrix[4], matrix[8], matrix[12],
                        matrix[1], matrix[5], matrix[9], matrix[13],
                        matrix[2], matrix[6], matrix[10], matrix[14],
                        matrix[3], matrix[7], matrix[11], matrix[15]
        ));
    }
}
