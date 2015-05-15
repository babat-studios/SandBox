precision mediump float;

/* Lighting */
uniform vec4 gEyePosition;
uniform vec4 gLightPosition;
uniform vec4 gLightColor;

/* Material */
uniform vec4 gAmbientColor;
uniform vec4 gDiffuseColor;
uniform vec4 gSpecularColor;
uniform float gShininess;

/* Texture */
uniform sampler2D gTexture;

/* Interpolation data */
varying vec4 point;
varying vec4 normal;
varying vec2 uv;

void main (void)  
{  
    vec3 L = (gLightPosition - point).xyz;
    vec3 N = normal.xyz;

    L = normalize(L);
    N = normalize(N);

    vec3 R = reflect(L,N);
    R = normalize(R);

    vec3 V = (gEyePosition - point).xyz;
    V = normalize(V);

    float cosTheta = clamp(dot(N, L), 0, 1);
    float cosAlpha = dot(R, V);

    vec4 ka = clamp(gAmbientColor * gLightColor, 0, 1);
    vec4 kd = clamp(gDiffuseColor * gLightColor * cosTheta, 0, 1);
    vec4 ks = clamp(gSpecularColor * gLightColor * pow(cosAlpha, gShininess), 0, 1);

    gl_FragColor = clamp(ka + kd, 0, 1);
}
