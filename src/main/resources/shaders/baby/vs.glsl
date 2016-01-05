/*
 * Vertex shader.
 */

#version 130

const int NUM_LIGHTS = 6;
uniform vec3 lightPosition[NUM_LIGHTS];

varying vec3 vertexPositionMV;
varying vec3 vertexNormal;
varying vec3 vertexLightPositionMV[NUM_LIGHTS];

void main()
{
    gl_Position = ftransform();

    vertexPositionMV = vec3(gl_ModelViewMatrix * gl_Vertex);
    vertexNormal = gl_NormalMatrix * gl_Normal;
    for (int i = 0; i < NUM_LIGHTS; i++) {
        vertexLightPositionMV[i] = vec3(gl_ModelViewMatrix * vec4(lightPosition[i], 1.0));
    }
}
