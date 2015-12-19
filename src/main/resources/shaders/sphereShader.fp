uniform vec4  lightPosition1;
uniform vec4  lightPosition2;
uniform vec4  lightPosition3;
uniform vec4  lightPosition4;
uniform vec4  lightPosition5;
uniform vec4  lightPosition6;

uniform vec4 myColor;

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
   // Compute normalized vector from vertex to light in eye space  (Leye)
   vec3 Leye = normalize(lightPosition1.xyz + lightPosition2.xyz + lightPosition3.xyz + lightPosition4.xyz + lightPosition5.xyz + lightPosition6.xyz - vPositionES);
   
   // Compute Veye
   vec3 Veye = -normalize(vPositionES);

   // Compute half-angle
   vec3 Heye = normalize(Leye + Veye);

   // N.L
   vec3 NdotL = vec3(dot (bump, Leye));
   
   // Compute N.H
   vec3 NdotH = vec3(dot (bump, Heye));
   
   //compute ambient light
   float ambient = 0.5;
   
   // "Half-Lambert" technique for more pleasing diffuse term
   vec3 diffuse =0.5 *max(vec3(0.0), NdotL); 
   
   // Compute specular light
   vec3 specular = pow(max(vec3(0.0),NdotH),vec3(40.0));
     
   //**--------------------------------------
   //** Compute global effect for each pixel
   //**--------------------------------------
   gl_FragColor = vec4 ((texture * (diffuse + ambient) + specular),1.0);
   //gl_FragColor = texture;
}