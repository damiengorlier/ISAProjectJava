/*
 * Vertex shader.
 */

#version 130

attribute vec3 attrTangent;

const int NUM_LIGHTS = 6;
uniform vec3 lightPosition[NUM_LIGHTS];

varying vec3 vPositionMV;
varying mat3 vTBN;
varying vec2 vTexCoord;
varying vec3 vLightPositionMV[NUM_LIGHTS];

void main()
{
    gl_Position = ftransform();

    // Transform position and normal to eye space
    vPositionMV  = vec3(gl_ModelViewMatrix * gl_Vertex);
    vTexCoord = vec2(gl_MultiTexCoord0);
    for (int i = 0; i < NUM_LIGHTS; i++) {
//        vLightPositionMV[i] = vec3(gl_ModelViewMatrix * vec4(lightPosition[i], 1.0));
        vLightPositionMV[i] = vec3(gl_ModelViewMatrix * vec4(lightPosition[i], 1.0));
    }

    vec3 normal = -normalize(gl_NormalMatrix * gl_Normal);

    vec3 tangent = -normalize(gl_NormalMatrix * attrTangent);

    vec3 binormal = normalize(cross(normal, tangent));

    vTBN = mat3(tangent, binormal, normal);
}
