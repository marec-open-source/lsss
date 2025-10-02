uniform sampler2D data;
uniform sampler1D colormap;
uniform int actualNy;

void main(void)
{
   vec4 textureCoordinates = gl_TexCoord[0];

   float repackedNy = ceil(float(actualNy) / 4.0) * 4.0;
   int i = int(textureCoordinates.s * float(actualNy));
   float s = (float(i) + 0.5) / float(repackedNy);

   vec4 color = texture2D(data, vec2(s , textureCoordinates.t));

   int channel = int(mod(float(i), 4.0));

   float value;
   if (channel == 0)
   {
      value = color.r;
   }
   else if (channel == 1)
   {
      value = color.g;
   }
   else if (channel == 2)
   {
      value = color.b;
   }
   else
   {
      value = color.a;
   }

   gl_FragColor = texture1D(colormap, value);
}
