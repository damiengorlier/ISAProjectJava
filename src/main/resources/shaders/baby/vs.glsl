/*
 * Vertex shader.
 */

#version 130

varying vec3 vertexPosition;
varying vec3 vertexPositionMV;
varying vec3 vertexNormal;

void main()
{
    gl_Position = ftransform();

    vertexPosition = vec3(gl_Vertex);
    vertexPositionMV = vec3(gl_ModelViewMatrix * gl_Vertex);
    vertexNormal = gl_NormalMatrix * gl_Normal;
}
