//uniform vec3  lightPosition1;
//uniform vec3  lightPosition2;
//uniform vec3  lightPosition3;
//uniform vec3  lightPosition4;
//uniform vec3  lightPosition5;
//uniform vec3  lightPosition6;

const int numLight = 3;

uniform lightVector[numLight];

uniform vec4  eyePosition;

uniform sampler2D uterusTexture, uterusBumpMap;

varying vec3 vPositionES;

varying vec2 vTexCoord;


void main(void)
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
 
   //**---------------------
   //**Compute light effect
   //**---------------------

   //compute ambient light
   vec3 ambient = vec3(0.5, 0.5,0.5);
   ambient *= texture;

   // diffuse
   vec3 diffuse = (0.0,0.0,0.0);
   
   //specular
   vec3 specular = (0.0,0.0,0.0);
   
   // Compute Veye
   vec3 Veye = -normalize(vPositionES);
   
   for (int i = 0; i < numLight; ++i){

   // Compute normalized vector from vertex to light in eye space  (Leye)
   vec3 Leye = normalize(lightVector[i] - vPositionES);   

   // Compute half-angle
   vec3 Heye = normalize(Leye + Veye);

   // N.L
   vec3 NdotL = vec3(dot (bump, Leye));

   // Compute N.H
   vec3 NdotH = vec3(dot (bump, Heye));   
   
   //diffuse
   diffuse +=  texture * (0.5 *max(vec3(0.0), NdotL));
   
   // specular
   specular += pow(max(vec3(0.0),NdotH),vec3(40.0));
     
   }
      

   //**--------------------------------------
   //** Compute global effect for each pixel
   //**--------------------------------------
   gl_FragColor = vec4 (diffuse + ambient + specular,1.0);
   //gl_FragColor = texture;
}