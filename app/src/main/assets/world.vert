/* Transformations */
uniform mat4 gModelMatrix;
uniform mat4 gViewMatrix;
uniform mat4 gProjectionMatrix;
uniform mat4 gNormalMatrix;

/* Vertex attributes */
attribute vec4 gVertexPosition;
attribute vec4 gVertexNormal;

/* Interpolation data */
varying vec4 point;
varying vec4 normal;

void main(void)
{
    point = gModelMatrix * gVertexPosition;
    normal = gNormalMatrix * vec4(gVertexNormal.xyz, 0);

    gl_Position = gProjectionMatrix * gViewMatrix * gModelMatrix * gVertexPosition;
}
