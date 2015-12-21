/*
 * Fragment shader.
 */

#version 130

const int NUM_LIGHTS = 6;
const vec3 AMBIENT = vec3(0.1, 0.1, 0.1);
const vec3 LIGHT_COLOR = vec3(1.0, 1.0, 1.0);
const float SHININESS_COEFF = 64.0;
const float MATERIAL_THICKENESS = 0.6;
const vec3 EXTINCTION_COEFF = vec3(0.8, 0.12, 0.2);
const vec4 BASE_COLOR = vec4(225.0/255.0, 76.0/255.0, 60.0/255.0, 1.0);
const vec3 SPECULAR_COLOR = vec3(0.9, 0.9, 0.9);
const float RIM_SCALAR = 1.0;

// Attenuation factors : 1 / (Kc + Kl * dist + Kq * dist²)
const float Kc = 0.0;
const float Kl = 0.2;
const float Kq = 0.01;

uniform vec3 lightPosition[NUM_LIGHTS];

varying vec3 vertexPosition;
varying vec3 vertexPositionMV;
varying vec3 vertexNormal;

float distanceAttenuation(vec3 lightVector);
float halfLambert(vec3 vect1, vec3 vect2);

void main()
{
    // Normalize interpolated normal
    vec3 Neye = normalize(vertexNormal);

    // Compute Veye
    vec3 Veye = -normalize(vertexPositionMV);

    vec3 diffuse = vec3(0.0, 0.0, 0.0);
    vec3 wax = vec3(0.0, 0.0, 0.0);
    vec3 specular = vec3(0.0, 0.0, 0.0);

    for(int i = 0; i < NUM_LIGHTS; i++) {

        vec3 lightVector = lightPosition[i].xyz - vertexPosition;

        float attFactor = distanceAttenuation(lightVector);

        vec3 Leye = normalize(lightVector);

        // Compute half-angle
        vec3 Heye = normalize(Leye + Veye);

        // N.L
        float NdotL = halfLambert(Leye,Neye);

        // Compute N.H
        float NdotH = dot(Neye,Heye);

        float waxAttenuation = 8 * (1.0 / length(lightVector));

        vec3 indirectLightComponent = vec3(MATERIAL_THICKENESS * max(0.0,dot(-Neye,Leye)));
        indirectLightComponent += MATERIAL_THICKENESS * halfLambert(-Veye,Leye);
        indirectLightComponent *= waxAttenuation;
        indirectLightComponent.r *= EXTINCTION_COEFF.r;
        indirectLightComponent.g *= EXTINCTION_COEFF.g;
        indirectLightComponent.b *= EXTINCTION_COEFF.b;

        vec3 rim = vec3(1.0 - max(0.0,dot(Neye,Veye)));
        rim *= rim;
        rim *= max(0.0,dot(Neye,Leye)) * SPECULAR_COLOR;

        diffuse += BASE_COLOR.rgb * (clamp(NdotL, 0.0, 1.0)) * attFactor * BASE_COLOR.a;

        wax += indirectLightComponent + (rim * RIM_SCALAR * waxAttenuation * BASE_COLOR.a);

        specular += SPECULAR_COLOR * pow(clamp(NdotH, 0.0, 1.0), SHININESS_COEFF) * attFactor * BASE_COLOR.a;
    }

    gl_FragColor = vec4(clamp(AMBIENT + diffuse + wax + specular, 0.0, 1.0), BASE_COLOR.a);
}

float distanceAttenuation(vec3 lightVector) {
    float dist = length(lightVector);
    float attFactor = 1.0 / (Kc + Kl * dist + Kq * dist * dist);
    return attFactor;
}

float halfLambert(vec3 vect1, vec3 vect2)
{
    float product = dot(vect1,vect2);
    return product * 0.5 + 0.5;
}
