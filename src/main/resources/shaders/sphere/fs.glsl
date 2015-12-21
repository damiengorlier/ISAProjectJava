/*
 * Fragment shader.
 */

#version 130

const int NUM_LIGHTS = 6;
const vec3 AMBIENT = vec3(0.1, 0.1, 0.1);
const vec3 LIGHT_COLOR = vec3(1.0, 1.0, 1.0);
const float SHININESS_COEFF = 40.0;

// Attenuation factors : 1 / (Kc + Kl * dist + Kq * dist²)
const float Kc = 0.0;
const float Kl = 0.05;
const float Kq = 0.03;

uniform vec3 lightPosition[NUM_LIGHTS];
uniform sampler2D uterusTexture, uterusBumpMap;

varying vec3 vPositionES;
varying vec3 vPosition;
varying vec2 vTexCoord;

float distanceAttenuation(vec3 lightVector);

void main()
{
    //**------------------------
    //** Compute texture effect
    //**------------------------

    vec3 texture = texture2D( uterusTexture, vTexCoord ).xyz;
    vec3 bump = texture2D( uterusBumpMap, vTexCoord ).xyz;

    //**--------------------------------------------------------
    //** "Smooth out" the bumps based on the bumpiness parameter.
    //** This is simply a linear interpolation between a "flat"
    //** normal and a "bumped" normal.  Note that this "flat"
    //** normal is based on the texture space coordinate basis.
    //**--------------------------------------------------------
    vec3 smoothOut = vec3(0.5, 0.5, 1.0);
    float bumpiness = 0.5;

    bump = mix( smoothOut, bump, bumpiness );
    bump = normalize( ( bump * 2.0 ) - 1.0 );

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
        float NdotL = dot(bump, Leye);

        // Compute N.H
        float NdotH = dot(bump,Heye);

        diffuse += LIGHT_COLOR * clamp(NdotL, 0.0, 1.0) * attFactor;

        specular += LIGHT_COLOR * pow(clamp(NdotH, 0.0, 1.0), SHININESS_COEFF) * attFactor;
    }

    gl_FragColor = vec4(clamp(texture * (diffuse + AMBIENT) + specular, 0.0, 1.0), 1.0);
}

float distanceAttenuation(vec3 lightVector) {
    float dist = length(lightVector);
    float attFactor = 1.0 / (Kc + Kl * dist + Kq * dist * dist);
    return attFactor;
}
