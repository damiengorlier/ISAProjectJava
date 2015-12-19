/*
 * Vertex shader.
 */

#version 130

void main() {
    gl_FrontColor = gl_Color;
    gl_Position = ftransform();
}
