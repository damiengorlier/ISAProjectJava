//uniform vec3  lightPosition1;
//uniform vec3  lightPosition2;
//uniform vec3  lightPosition3;
//uniform vec3  lightPosition4;
//uniform vec3  lightPosition5;
//uniform vec3  lightPosition6;

const int numLight = 3;

uniform lightVector[numLight];

uniform vec3 eyePosition;

varying vec3 vPositionES;
varying vec3 vNormalES;

vec3 myColor = vec4(254.0/255.0, 195.0/255.0, 172.0/255.0); //définition de la couleur chair

void main(void)
{

   //compute ambient light
   vec3 ambient = vec3(0.5,0.5,0.5);
   ambient *= myColor;
   
   //diffuse
   vec3 diffuse = vec3(0.0,0.0,0.0);
   float waxiness = 0.5;

   //specular
   vec3 specular = vec3(0.0,0.0,0.0);   

   // Compute Veye
   vec3 Veye = -normalize(vPositionES);

   for (int i =0 ; i<numLight; ++i){
   // Compute normalized vector from vertex to light in eye space  (Leye)
   vec3 Leye = normalize(lightVector[i] - vPositionES);   
   
   // Compute half-angle
   vec3 Heye = normalize(Leye + Veye);
   
   // N.L
   float NdotL = dot(Neye, Leye);

   // Compute N.H
   float NdotH = dot(Neye,Heye);   
   
   // "Half-Lambert" technique for more pleasing diffuse term
   diffuse += myColor *( 0.5 *(waxiness+(1-waxiness)* NdotL)); // paas certaine que cette ligne fonctionne (verifier s'il ne faut pas changer le type de la partie droite de l'équation)
   
   // Compute specular light
   float specular += pow(max(vec3(0.0),NdotH),vec3(40.0));
   }


   
   gl_FragColor = myColor * (diffuse+ambient) + specular;
   
}