/*
 * Fragment shader.
 */

#version 130

const int NUM_LIGHTS = 6;
const vec4 AMBIENT = vec4(0.1, 0.1, 0.1, 1.0);
const vec3 LIGHT_COLOR = vec3(1.0, 1.0, 1.0);

uniform vec3 lightPosition[NUM_LIGHTS];
uniform vec3 eyePosition;

varying vec3 fragNormal;
varying vec3 fragPosition;

void main()
{
//    //calculate normal in world coordinates
//    vec3 normal = normalize(fragNormal);
//
//    //calculate the vector from this pixels surface to the light source
//    vec3 surfaceToLight = lightPosition[0] - fragPosition;
//
//    //calculate the cosine of the angle of incidence
//    float brightness = dot(normal, surfaceToLight) / (length(surfaceToLight) * length(normal));
//    brightness = clamp(brightness, 0, 1);
//
//    //calculate final color of the pixel, based on:
//    // 1. The angle of incidence: brightness
//    // 2. The color/intensities of the light: light.intensities
//    // 3. The texture and texture coord: texture(tex, fragTexCoord)
////    vec4 surfaceColor = texture(tex, fragTexCoord);
//    vec4 surfaceColor = {1.0, 1.0, 1.0, 1.0};
//    vec3 lightColor = {1.0, 1.0, 1.0};
//    vec4 diffuse = vec4(brightness * lightColor * surfaceColor.rgb, surfaceColor.a);
//    gl_FragColor = AMBIENT + diffuse;

    vec3 Leye = normalize(lightPosition[0].xyz - fragPosition);

    // Normalize interpolated normal
    vec3 Neye = normalize(fragNormal);

    // Compute Veye
    vec3 Veye = -normalize(fragPosition);

    // Compute half-angle
    vec3 Heye = normalize(Leye + Veye);

    // N.L
    float NdotL = dot(Neye, Leye);

    // Compute N.H
    float NdotH = dot(Neye,Heye);

    //compute ambient light
    float ambient = 0.5;

    // "Half-Lambert" technique for more pleasing diffuse term
    float diffuse = NdotL * 0.5;


    // Compute specular light
    float specular = pow(clamp(NdotH, 0.0, 1.0),40.0);

    //**--------------------------------------
    //** Compute global effect for each pixel
    //**--------------------------------------
    gl_FragColor = vec4(diffuse + ambient + specular);
}
