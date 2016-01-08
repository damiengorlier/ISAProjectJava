/*
 * Fragment shader of the sphere.
 */

#version 130

const int NUM_LIGHTS = 6;
const vec3 LIGHT_COLOR = vec3(1.0, 1.0, 1.0);
const vec3 SPECULAR_COLOR = vec3(0.9, 0.9, 0.9);
const float SHININESS_COEFF = 200.0;
const float LIGHT_RADIUS = 7;
const float AMBIENT_COEFF = 0.1;

// Attenuation factors : 1 / (Kc + Kl * dist + Kq * dist²)
const float Kc = 1.0;
const float Kl = 2.0/LIGHT_RADIUS;
const float Kq = 1.0/(LIGHT_RADIUS*LIGHT_RADIUS);

uniform sampler2D uterusTexture, uterusBumpMap;

varying vec3 vPositionMV;
varying mat3 vTBN;
varying vec2 vTexCoord;
varying vec3 vLightPositionMV[NUM_LIGHTS];

float distanceAttenuation(vec3 lightVector);

void main()
{
    //**------------------------
    //** Compute texture effect
    //**------------------------

    vec4 texture = texture2D( uterusTexture, vTexCoord );
    vec3 normal = texture2D( uterusBumpMap, vTexCoord ).xyz;

    normal = normalize( ( normal * 2.0 ) - 1.0 );
    normal = normalize(vTBN * normal);

    // Compute Veye
    vec3 Veye = -normalize(vTBN * vPositionMV);

    vec3 diffuse = vec3(0.0, 0.0, 0.0);
    vec3 specular = vec3(0.0, 0.0, 0.0);

	vec3 ambient = AMBIENT_COEFF * texture.rgb;
//	ambient *= texture.rgb;

    for(int i = 0; i < NUM_LIGHTS; i++) {

        vec3 lightVector = vTBN * (vLightPositionMV[i] - vPositionMV);

        float attFactor = distanceAttenuation(lightVector);
		//float attFactor = 1.0;

        vec3 Leye = normalize(lightVector);

        // Compute half-angle
        vec3 Heye = normalize(Leye + Veye);

        // N.L
        float NdotL = dot(normal, Leye);

        // Compute N.H
        float NdotH = dot(normal, Heye);

        diffuse += texture.rgb * (clamp(NdotL, 0.0, 1.0)) * attFactor * texture.a;

        specular += SPECULAR_COLOR * pow(clamp(NdotH, 0.0, 1.0), SHININESS_COEFF) * attFactor * texture.a;
    }

    gl_FragColor = vec4(clamp(diffuse + ambient + specular, 0.0, 1.0), texture.a);
}

float distanceAttenuation(vec3 lightVector) {
    float dist = length(lightVector);
    float attFactor = 1.0 / (Kc + Kl * dist + Kq * dist * dist);
    return attFactor;
}
