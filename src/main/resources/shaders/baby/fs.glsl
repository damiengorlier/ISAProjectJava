/*
 * Fragment shader.
 */

#version 130

const int NUM_LIGHTS = 6;
const vec3 LIGHT_COLOR = vec3(1.0, 1.0, 1.0);
const float SHININESS_COEFF = 200.0;
const float MATERIAL_THICKENESS = 0.6;
const vec3 EXTINCTION_COEFF = vec3(0.8, 0.12, 0.2);
const vec3 SPECULAR_COLOR = vec3(0.9, 0.9, 0.9);
const float RIM_SCALAR = 1.0;
const float LIGHT_RADIUS = 15;
const float AMBIENT_COEFF = 0.4;

// Attenuation factors : 1 / (Kc + Kl * dist + Kq * distÂ²)
const float Kc = 1.0;
const float Kl = 2/LIGHT_RADIUS;
const float Kq = 1/(LIGHT_RADIUS*LIGHT_RADIUS);

uniform sampler2D uterusTexture;

varying vec3 vertexPositionMV;
varying vec3 vertexNormal;
varying vec3 vertexLightPositionMV[NUM_LIGHTS];

float distanceAttenuation(vec3 lightVector);
float halfLambert(vec3 vect1, vec3 vect2);

vec4 BASE_COLOR = vec4(100.0/255.0, 75.0/255.0, 25.0/255.0, 1.0);

void main()
{
    // Normalize interpolated normal
    vec3 Neye = normalize(vertexNormal);

    // Compute Veye
    vec3 Veye = -normalize(vertexPositionMV);

    vec3 diffuse = vec3(0.0, 0.0, 0.0);
    vec3 wax = vec3(0.0, 0.0, 0.0);
    vec3 specular = vec3(0.0, 0.0, 0.0);
	vec4 reflection = vec4(0.0,0.0,0.0,0.0);
	vec4 refraction = vec4(0.0,0.0,0.0,0.0);
	
	vec3 reflectedDirection = -reflect(Veye, Neye);
	vec3 refractedDirection = -refract(Veye,Neye,0.75);

	float m = 2.0 * sqrt(pow(reflectedDirection.x, 2.0) + pow (reflectedDirection.y, 2.0) + pow(reflectedDirection.z+1.0 , 2.0));
	
	reflection = texture(uterusTexture,reflectedDirection.xy/m+0.5);
	refraction = texture(uterusTexture,refractedDirection.xy/m+0.5);
	
	BASE_COLOR *= reflection;
	//BASE_COLOR *= refraction;
	
	vec3 ambient = AMBIENT_COEFF * vec3(1.0, 1.0, 1.0);
	ambient *= BASE_COLOR.rgb;
	//ambient *= reflection.rgb;

    for(int i = 0; i < NUM_LIGHTS; i++) {

        vec3 lightVector = vertexLightPositionMV[i].xyz - vertexPositionMV;

        float attFactor = distanceAttenuation(lightVector);

        vec3 Leye = normalize(lightVector);

        // Compute half-angle
        vec3 Heye = normalize(Leye + Veye);

        // N.L
        float NdotL = dot(Neye, Leye);

        // Compute N.H
        float NdotH = dot(Neye,Heye);

        float waxAttenuation = 7 * (1.0 / length(lightVector));

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
		//diffuse += reflection.rgb * (clamp(NdotL, 0.0, 1.0)) * attFactor * reflection.a;

        wax += (indirectLightComponent + (rim * RIM_SCALAR * waxAttenuation * BASE_COLOR.a)) * attFactor * BASE_COLOR.a;
		//wax += indirectLightComponent + (rim * RIM_SCALAR * waxAttenuation * reflection.a);

        specular += SPECULAR_COLOR * pow(clamp(NdotH, 0.0, 1.0), SHININESS_COEFF) * attFactor * BASE_COLOR.a;
		//specular += SPECULAR_COLOR * pow(clamp(NdotH, 0.0, 1.0), SHININESS_COEFF) * attFactor * reflection.a;
    }

	//gl_FragColor = refraction;
	
	//gl_FragColor = reflection;
	
    gl_FragColor = vec4(clamp(ambient + diffuse + wax + specular, 0.0, 1.0), BASE_COLOR.a);
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
