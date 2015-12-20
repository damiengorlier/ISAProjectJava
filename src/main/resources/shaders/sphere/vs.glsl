/*
 * Vertex shader.
 */

#version 130

varying vec3 fragNormal;
varying vec3 fragPosition;

void main()
{
    fragNormal = gl_NormalMatrix * gl_Normal;
    fragPosition = vec3(gl_ModelViewMatrix * gl_Vertex);

    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
