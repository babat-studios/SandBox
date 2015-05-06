uniform vec4 gLightPosition;

uniform mat4 gViewProjectionMatrix;
uniform mat4 gModelMatrix;
uniform mat4 gNormalMatrix;

uniform vec4 gAmbientColor;
uniform vec4 gDiffuseColor;

attribute vec4 gVertexPosition;
attribute vec4 gVertexNormal;

varying vec4 vVertexColor;

void main() {
    vec4 light_dir = normalize((gViewProjectionMatrix * gLightPosition) - (gViewProjectionMatrix * gModelMatrix * gVertexPosition));
    vec4 normal = normalize(gNormalMatrix * vec4(vec3(gVertexNormal), 0));

    float incidence = dot(vec3(light_dir), vec3(normal));

    vVertexColor = (gDiffuseColor * incidence);

    // Debugging normal: Green if 0
    // if (length(normal) == 0) {
    //     vVertexColor = vec4(0.0f, 1.0f, 0.0f, 1.0f);
    // }

    // Debugging light dir: Blue if 0
    // else if (length(light_dir) == 0) {
    //     vVertexColor = vec4(0.0f, 0.0f, 1.0f, 1.0f);
    // }

    // Debugging incidence angle: Red if negative
    // else if (incidence < 0) {
    //     vVertexColor = vec4(1.0f, 0.0f, 0.0f, 1.0f);
    // }

    gl_Position = gViewProjectionMatrix * gModelMatrix * gVertexPosition;
}
