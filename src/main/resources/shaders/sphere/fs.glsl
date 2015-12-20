/*
 * Fragment shader.
 */

#version 130

const int NUM_LIGHTS = 6;
const vec3 AMBIENT = vec3(0.1, 0.1, 0.1);
const vec3 LIGHT_COLOR = vec3(1.0, 1.0, 1.0);

// Attenuation factors : 1 / (Kc + Kl * dist + Kq * dist²)
const float Kc = 0.0;
const float Kl = 0.05;
const float Kq = 0.03;

uniform vec3 lightPosition[NUM_LIGHTS];

varying vec3 vPositionES;
varying vec3 vPosition;
varying vec3 vNormalES;

float distanceAttenuation(vec3 lightVector);

void main()
{
    // Normalize interpolated normal
    vec3 Neye = normalize(vNormalES);

    // Compute Veye
    vec3 Veye = -normalize(vPositionES);

    vec3 diffuse = vec3(0.0, 0.0, 0.0);
    vec3 specular = vec3(0.0, 0.0, 0.0);

    for(int i = 0; i < NUM_LIGHTS; i++) {

        vec3 lightVector = lightPosition[i].xyz - vPosition;

        float attFactor = distanceAttenuation(lightVector);

        vec3 Leye = normalize(lightVector);

        // Compute half-angle
        vec3 Heye = normalize(Leye + Veye);

        // N.L
        float NdotL = dot(Neye, Leye);

        // Compute N.H
        float NdotH = dot(Neye,Heye);

        diffuse += LIGHT_COLOR * clamp(NdotL, 0.0, 1.0) * attFactor;

        specular += LIGHT_COLOR * pow(clamp(NdotH, 0.0, 1.0), 40.0) * attFactor;
    }

    vec4 sampl = vec4(1.0, 1.0, 1.0, 1.0);
    gl_FragColor = vec4(clamp(sampl.rgb * (diffuse + AMBIENT) + specular, 0.0, 1.0), sampl.a);
}

float distanceAttenuation(vec3 lightVector) {
    float dist = length(lightVector);
    float attFactor = 1.0 / (Kc + Kl * dist + Kq * dist * dist);
    return attFactor;
}
