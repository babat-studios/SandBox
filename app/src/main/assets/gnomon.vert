uniform mat4 transform;
attribute vec4 vertex;
attribute vec4 color;

varying vec4 finalColor;

void main() {
    finalColor = color;
    gl_Position = transform * vertex;
}
