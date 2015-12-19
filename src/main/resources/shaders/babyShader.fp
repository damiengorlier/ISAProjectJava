uniform vec3 lightPosition1;
uniform vec3 lightPosition2;
uniform vec3 lightPosition3;
uniform vec3 lightPosition4;
uniform vec3 lightPosition5;
uniform vec3 lightPosition6;

uniform vec3 eyePosition;

varying vec3 vPositionES;
varying vec3 vNormalES;

vec4 myColor = vec4(254.0/255.0, 195.0/255.0, 172.0/255.0, 1.0); //définition de la couleur chair

void main(void)
{

   // Compute normalized vector from vertex to light in eye space  (Leye)
   vec3 Leye = normalize(lightPosition1 + lightPosition2 + lightPosition3 + lightPosition4 + lightPosition5 + lightPosition6 - vPositionES);

   // Normalize interpolated normal
   vec3 Neye = normalize(vNormalES);
   
   // Compute Veye
   vec3 Veye = -normalize(vPositionES);

   // Compute half-angle
   vec3 Heye = normalize(Leye + Veye);
   
   // N.L
   float NdotL = dot(Neye, Leye);
   
   // Compute N.H
   float NdotH = dot(Neye,Heye);
   
   //compute ambient light
   float ambient = 0.5;
   
   // "Half-Lambert" technique for more pleasing diffuse term
   float waxiness = 0.5;
   float diffuse = 0.5 *(waxiness+(1-waxiness)* NdotL);
   
   // Compute specular light
   float specular = pow(clamp(dot(Neye, Heye), 0.0, 1.0),64.0);
   
   gl_FragColor = myColor * (diffuse+ambient) + specular;
   
}