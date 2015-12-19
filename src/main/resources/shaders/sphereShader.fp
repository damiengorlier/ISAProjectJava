uniform vec4  lightPosition1;
uniform vec4  lightPosition2;
uniform vec4  lightPosition3;
uniform vec4  lightPosition4;
uniform vec4  lightPosition5;
uniform vec4  lightPosition6;

uniform vec4  eyePosition;

varying vec3 vPositionES;
varying vec3 vNormalES;


void main(void)
{   
 
   //**---------------------
   //**Compute light effect
   //**---------------------
   // Compute normalized vector from vertex to light in eye space  (Leye)
   vec3 Leye = normalize(lightPosition1.xyz + lightPosition2.xyz + lightPosition3.xyz + lightPosition4.xyz + lightPosition5.xyz + lightPosition6.xyz - vPositionES);

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
   float diffuse = NdotL * 0.5;
   
   
   // Compute specular light
   float specular = pow(clamp(NdotH, 0.0, 1.0),40.0);
     
   //**--------------------------------------
   //** Compute global effect for each pixel
   //**--------------------------------------
   gl_FragColor = vec4(diffuse + ambient + specular);
   
}