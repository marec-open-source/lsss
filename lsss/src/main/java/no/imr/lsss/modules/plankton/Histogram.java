package no.imr.lsss.modules.plankton;

import no.imr.tools.Utils;

/**
 * A histogram represented by arrays.
 */
final class Histogram {
   private float weight; // Used when combining histograms
   private float[] dividers = Utils.EMPTY_FLOAT_ARRAY;
   private float[] values = Utils.EMPTY_FLOAT_ARRAY;

   Histogram() {
   }

   Histogram(float[] dividers, float[] values) {
      weight = 1;
      this.dividers = dividers;
      this.values = values;
   }

   private static Histogram fromBinCount(float min, float max, int binCount) {
      float[] dividers = new float[binCount + 1];
      float[] values = new float[binCount];

      float binSize = (max - min) / binCount;
      for (int i = 0; i <= binCount; i++) {
         dividers[i] = min + i * binSize;
      }
      return new Histogram(dividers, values);
   }

   float[] getDividers() {
      return dividers;
   }

   float[] getValues() {
      return values;
   }

   void accumulate(Histogram histogram, float weightFactor) {
      accumulate(histogram.dividers, histogram.values, histogram.weight * weightFactor);
   }

   void accumulate(float[] otherDividers, float[] otherValues, float otherWeight) {
      if (otherValues.length == 0) {
         return;
      }

      if (values.length == 0) {
         dividers = otherDividers.clone();
         values = otherValues.clone();
         weight = otherWeight;
         return;
      }

      int count = combinedDividerCount(dividers, otherDividers);
      float[] newDividers = new float[count];
      float[] newValues = new float[count - 1];

      float weightB = otherWeight;
      float weightA = weight;
      float weightSum = weightB + weightA;

      float[] dividersA = dividers;
      float[] dividersB = otherDividers;
      float[] valuesA = values;
      float[] valuesB = otherValues;

      int iA = -1;
      int iB = -1;

      //Distribute dividers
      for (int i = 0; i < newDividers.length; i++) {
         float dNextA = iA + 1 < dividersA.length ? dividersA[iA + 1] : Float.POSITIVE_INFINITY;
         float dNextB = iB + 1 < dividersB.length ? dividersB[iB + 1] : Float.POSITIVE_INFINITY;
         float d;
         if (dNextA < dNextB) {
            d = dNextA;
            iA++;
         } else if (dNextA == dNextB) {
            d = dNextA;
            iA++;
            iB++;
         } else { // dNextA > dNextB
            d = dNextB;
            iB++;
         }
         newDividers[i] = d;
      }
      newDividers[newDividers.length - 1] = Math.max(dividersA[dividersA.length - 1], dividersB[dividersB.length - 1]);

      // Add values from A
      resampleHistogram(newDividers, newValues, dividersA, valuesA, weightA, weightSum);
      // Add values from B
      resampleHistogram(newDividers, newValues, dividersB, valuesB, weightB, weightSum);
      dividers = newDividers;
      values = newValues;
      weight = weightSum;
   }

   private static void resampleHistogram(float[] dividers, float[] values,
                                         float[] dividersOrig, float[] valuesOrig,
                                         float weight, float weightSum) {
      int iOrig = 0;
      float minOrig = dividersOrig[iOrig];
      float maxOrig = dividersOrig[iOrig + 1];
      for (int i = 0; i < dividers.length - 1; i++) {
         float min = dividers[i];
         float max = dividers[i + 1];
         while (maxOrig <= min && iOrig < dividersOrig.length - 2) {
            iOrig++;
            minOrig = dividersOrig[iOrig];
            maxOrig = dividersOrig[iOrig + 1];
         }
         if (minOrig >= max) continue;
         if (maxOrig <= min) continue;
         float overlap = Math.min(maxOrig, max) - Math.max(minOrig, min);
         float fractionA = overlap / (maxOrig - minOrig);
         float vA = valuesOrig[iOrig] * fractionA;
         values[i] += weightSum != 0 ? weight * vA / weightSum : 0;
      }
   }

   static int combinedDividerCount(float[] dividersA, float[] dividersB) {
      int count = 0;
      int iA = 0;
      int iB = 0;
      while (true) {
         if (iA == dividersA.length) return count + dividersB.length - iB;
         if (iB == dividersB.length) return count + dividersA.length - iA;

         float dA = dividersA[iA];
         float dB = dividersB[iB];
         if (dA <= dB) iA++;
         if (dB <= dA) iB++;
         count++;
      }
   }

   Histogram toFixedBinHistogram(float binSize) {
      if (dividers.length == 0) {
         return new Histogram();
      }

      int jMin = (int) Math.floor(dividers[0] / binSize);
      int jMax = (int) Math.ceil(dividers[dividers.length - 1] / binSize);

      Histogram histogram = fromBinCount(jMin * binSize, jMax * binSize, jMax - jMin);
      histogram.weight = weight;

      for (int i = 0; i < values.length; i++) {
         float cellMin = dividers[i];
         float cellMax = dividers[i + 1];
         float valueDensity = values[i] / (cellMax - cellMin);

         int j0 = (int) Math.floor(cellMin / binSize);
         int j1 = (int) Math.ceil(cellMax / binSize);
         for (int j = j0; j < j1; j++) {
            float newCellMin = j * binSize;
            float newCellMax = newCellMin + binSize;

            float overlap = Math.min(cellMax, newCellMax) - Math.max(cellMin, newCellMin);
            histogram.values[j - jMin] += valueDensity * overlap;
         }
      }

      return histogram;
   }
}
