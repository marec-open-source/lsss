package no.imr.lsss.server.pojo.ping;

import no.imr.korona.data.ping.items.channel.ComplexChannelData;
import no.imr.tools.math.ComplexArray;

import java.util.Arrays;

public final class PojoComplexArray {
   public float[] re;
   public float[] im;

   public PojoComplexArray(ComplexArray complexArray, int beginIndex, int endIndex) {
      int n = endIndex - beginIndex;
      re = new float[n];
      im = new float[n];
      for (int i = 0; i < n; i++) {
         re[i] = (float) complexArray.re(beginIndex + i);
         im[i] = (float) complexArray.im(beginIndex + i);
      }
   }

   public PojoComplexArray(int sector, ComplexChannelData complexChannelData, int beginIndex, int endIndex) {
      re = Arrays.copyOfRange(complexChannelData.getReal()[sector], beginIndex, endIndex);
      im = Arrays.copyOfRange(complexChannelData.getImag()[sector], beginIndex, endIndex);
   }
}
