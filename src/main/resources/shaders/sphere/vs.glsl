/*
 * Vertex shader.
 */

#version 130

varying vec3 vPositionES;
varying vec3 vNormalES;

void main()
{
    gl_Position = ftransform();

    // Transform position and normal to eye space
    vPositionES  = vec3(gl_ModelViewMatrix * gl_Vertex);
    vNormalES    = gl_NormalMatrix * gl_Normal;
}
