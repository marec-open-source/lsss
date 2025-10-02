package no.imr.lsss.modules.sv;

import no.imr.tools.math.ArrayMath;

import java.util.Arrays;

final class PerFrequencyData {
   final int[] histogram = new int[SvDistributionModule.N_LOG_SV];
   double svSum;
   int count;

   PerFrequencyData() {
   }

   static PerFrequencyData[] newArray(int length) {
      PerFrequencyData[] array = new PerFrequencyData[length];
      for (int i = 0; i < length; i++) {
         array[i] = new PerFrequencyData();
      }
      return array;
   }

   void clear() {
      Arrays.fill(histogram, 0);
      svSum = 0;
      count = 0;
   }

   void accumulate(PerFrequencyData perFrequencyData) {
      ArrayMath.add(histogram, perFrequencyData.histogram);
      svSum += perFrequencyData.svSum;
      count += perFrequencyData.count;
   }

   float getMeanSv() {
      return (float) (svSum / count);
   }
}
