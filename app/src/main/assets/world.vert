/* Lighting */
uniform vec4 gEyePosition;
uniform vec4 gLightPosition;
uniform vec4 gLightColor;

/* Transformations */
uniform mat4 gModelMatrix;
uniform mat4 gViewMatrix;
uniform mat4 gProjectionMatrix;
uniform mat4 gNormalMatrix;

/* Vertex attributes */
attribute vec4 gVertexPosition;
attribute vec4 gVertexNormal;

/* Material */
uniform vec4 gAmbientColor;
uniform vec4 gDiffuseColor;
uniform vec4 gSpecularColor;
uniform float gShininess;

/* Interpolation data */
varying vec4 color;

void main(void)
{     
    vec3 L = (gLightPosition - (gModelMatrix * gVertexPosition)).xyz;
    vec3 N = (gNormalMatrix * vec4(gVertexNormal.xyz, 0)).xyz;

    L = normalize(L);
    N = normalize(N);

    vec3 R = reflect(L,N);
    R = normalize(R);

    vec3 V = (gEyePosition - (gModelMatrix * gVertexPosition)).xyz;
    V = normalize(V);

    float cosTheta = clamp(dot(N, L), 0, 1);
    float cosAlpha = dot(R, V);

    vec4 ka = (gAmbientColor * gLightColor);
    vec4 kd = (gDiffuseColor * gLightColor * cosTheta);
    vec4 ks = (gSpecularColor * gLightColor * pow(cosAlpha, gShininess));

    color = ka + kd + ks;

    // Debugging R: Green if 0
    if (length(R) == 0) {
        color = vec4(0.0f, 1.0f, 0.0f, 1.0f);
    }

    // Debugging V: Blue if 0
    else if (length(V) == 0) {
        color = vec4(0.0f, 0.0f, 1.0f, 1.0f);
    }

    // Debugging cosAlpha: Red if negative
    else if (cosAlpha < 0) {
        color = vec4(1.0f, 0.0f, 0.0f, 1.0f);
    }

    gl_Position = gProjectionMatrix * gViewMatrix * gModelMatrix * gVertexPosition;
}
