precision mediump float;

varying vec4 vVertexColor;

void main() {
    gl_FragColor = vec4(vec3(vVertexColor), 1);
}