/*
 * Fragment shader.
 */

#version 130

const int NUM_LIGHTS = 6;
const vec3 LIGHT_COLOR = vec3(1.0, 1.0, 1.0);
const float SHININESS_COEFF = 40.0;
const float LIGHT_RADIUS = 50;
const float AMBIENT_COEFF = 0.5;

// Attenuation factors : 1 / (Kc + Kl * dist + Kq * dist²)
const float Kc = 1.0;
const float Kl = 2/LIGHT_RADIUS;
const float Kq = 1/(LIGHT_RADIUS*LIGHT_RADIUS);

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

    vec4 texture = texture2D( uterusTexture, vTexCoord );
    vec4 bump = texture2D( uterusBumpMap, vTexCoord );

    //**--------------------------------------------------------
    //** "Smooth out" the bumps based on the bumpiness parameter.
    //** This is simply a linear interpolation between a "flat"
    //** normal and a "bumped" normal.  Note that this "flat"
    //** normal is based on the texture space coordinate basis.
    //**--------------------------------------------------------
    vec4 smoothOut = vec4(0.5, 0.5, 1.0,1.0);
    float bumpiness = 0.8;

    bump = mix( smoothOut, bump, bumpiness );
    bump = normalize( ( bump * 2.0 ) - 1.0 );

    // Compute Veye
    vec3 Veye = -normalize(vPositionES);

    vec3 diffuse = vec3(0.0, 0.0, 0.0);
    vec3 specular = vec3(0.0, 0.0, 0.0);
	
	vec3 ambient = AMBIENT_COEFF * vec3(1.0, 1.0, 1.0);
	ambient *= texture.rgb;

    for(int i = 0; i < NUM_LIGHTS; i++) {

        vec3 lightVector = lightPosition[i].xyz - vPosition;

        float attFactor = distanceAttenuation(lightVector);
		//float attFactor = 1.0;

        vec3 Leye = normalize(lightVector);

        // Compute half-angle
        vec3 Heye = normalize(Leye + Veye);

        // N.L
        float NdotL = dot(bump.rgb, Leye);

        // Compute N.H
        float NdotH = dot(-bump.rgb,Heye);

        diffuse += texture.rgb * (clamp(NdotL, 0.0, 1.0)) * attFactor*texture.a;

        specular += pow(clamp(NdotH, 0.0, 1.0), SHININESS_COEFF) * attFactor*texture.a;
    }

    gl_FragColor = vec4(clamp(diffuse + ambient+ specular, 0.0, 1.0), texture.w);
}

float distanceAttenuation(vec3 lightVector) {
    float dist = length(lightVector);
    float attFactor = 1.0 / (Kc + Kl * dist + Kq * dist * dist);
    return attFactor;
}
