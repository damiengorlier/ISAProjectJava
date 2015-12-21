#version 130

const int NUM_LIGHTS = 6;

const float MaterialThickness = 0.6;
const vec3 ExtinctionCoefficient = vec3(0.8, 0.12, 0.2); // Will show as X Y and Z ports in QC, but actually represent RGB values.
const vec4 LightColor = vec4(1.0, 1.0, 1.0, 1.0);
const vec4 BaseColor = vec4(254.0/255.0, 195.0/255.0, 172.0/255.0, 1);
const vec4 SpecColor = vec4(1.0, 0.9, 0.9, 1.0);
const float SpecPower = 64;
const float RimScalar = 1.0;

// Attenuation factors : 1 / (Kc + Kl * dist + Kq * dist²)
const float Kc = 0.0;
const float Kl = 0.05;
const float Kq = 0.03;

uniform vec3 lightPosition[NUM_LIGHTS];

varying vec3 worldNormal, eyeVec, lightVec, vertPos, lightPos;

float halfLambert(in vec3 vect1, in vec3 vect2)
{
    float product = dot(vect1,vect2);
    return product * 0.5 + 0.5;
}

float blinnPhongSpecular(in vec3 normalVec, in vec3 lightVec, in float specPower)
{
    vec3 halfAngle = normalize(normalVec + lightVec);
    return pow(clamp(0.0,1.0,dot(normalVec,halfAngle)),specPower);
}

// Main fake sub-surface scatter lighting function

vec4 subScatterFS()
{
    float attenuation = 10.0 * (1.0 / distance(lightPos,vertPos));
    vec3 eVec = normalize(eyeVec);
    vec3 lVec = normalize(lightVec);
    vec3 wNorm = normalize(worldNormal);

    vec4 dotLN = vec4(halfLambert(lVec,wNorm) * attenuation);
    //dotLN *= texture2D(Texture, gl_TexCoord[0].xy);
    dotLN *= BaseColor;

    vec3 indirectLightComponent = vec3(MaterialThickness * max(0.0,dot(-wNorm,lVec)));
    indirectLightComponent += MaterialThickness * halfLambert(-eVec,lVec);
    indirectLightComponent *= attenuation;
    indirectLightComponent.r *= ExtinctionCoefficient.r;
    indirectLightComponent.g *= ExtinctionCoefficient.g;
    indirectLightComponent.b *= ExtinctionCoefficient.b;

    vec3 rim = vec3(1.0 - max(0.0,dot(wNorm,eVec)));
    rim *= rim;
    rim *= max(0.0,dot(wNorm,lVec)) * SpecColor.rgb;

    vec4 finalCol = dotLN + vec4(indirectLightComponent,1.0);
    finalCol.rgb += (rim * RimScalar * attenuation * finalCol.a);
    finalCol.rgb += vec3(blinnPhongSpecular(wNorm,lVec,SpecPower) * attenuation * SpecColor * finalCol.a * 0.05);
    finalCol.rgb *= LightColor.rgb;

    return finalCol;
}

////////////////
//  MAIN LOOP //
////////////////

void main()
{
    gl_FragColor = subScatterFS();
}