#version 130

// Set light-position
const int NUM_LIGHTS = 6;
uniform vec3 lightPosition[NUM_LIGHTS];

// Varying variables to be sent to Fragment Shader
varying vec3 worldNormal, eyeVec, lightVec, vertPos, lightPos;

void subScatterVS(in vec4 ecVert)
{
    lightVec = lightPosition[0] - ecVert.xyz;
    eyeVec = -ecVert.xyz;
    vertPos = ecVert.xyz;
    lightPos = lightPosition[0];
}

void main()
{
    worldNormal = gl_NormalMatrix * gl_Normal;

    vec4 ecPos = gl_ModelViewProjectionMatrix * gl_Vertex;

    // Call function to set varyings for subscatter FS
    subScatterVS(ecPos);

    //Transform vertex by modelview and projection matrices
    gl_Position = ecPos;

    //Forward current texture coordinates after applying texture matrix
    gl_TexCoord[0] = gl_TextureMatrix[0] * gl_MultiTexCoord0;
}