package no.imr.korona.computation.plankton;

import no.imr.tools.Utils;

final class SizeHistogramRedistribution {
   private double[] groupStart = Utils.EMPTY_DOUBLE_ARRAY;
   private double[] groupEnd = Utils.EMPTY_DOUBLE_ARRAY;

   SizeHistogramRedistribution() {
   }

   boolean redistribute(SizeHistogram sizeHistogram) {
      return redistribute(sizeHistogram, Double.MIN_VALUE);
   }

   boolean redistribute(SizeHistogram sizeHistogram, double minBinSize) {
      double[] dividers = sizeHistogram.getDividers();
      double[] abundances = sizeHistogram.getAbundances();

      if (groupStart.length < abundances.length) {
         groupStart = new double[abundances.length];
         groupEnd = new double[abundances.length];
      }

      int groupCount = 0; // number of groups of non-empty cells.

      boolean previousEmpty = true;
      for (int i = 0; i <= abundances.length; i++) {
         boolean empty;
         boolean gap;
         if (i == abundances.length) {
            empty = true;
            gap = false;
         } else {
            empty = abundances[i] == 0;
            gap = i > 0 && dividers[2 * i] != dividers[2 * i - 1];
         }

         if (!previousEmpty && (empty || gap)) {
            groupEnd[groupCount] = dividers[2 * i - 1];
            groupCount++;
         }

         if (!empty && (previousEmpty || gap)) {
            groupStart[groupCount] = dividers[2 * i];
         }

         previousEmpty = empty;
      }

      if (groupCount == 0) {
         sizeHistogram.copyDividers(Utils.EMPTY_DOUBLE_ARRAY);
         return true;
      }

      double[] dividerBackup = dividers.clone();
      boolean ok = true;

      int dividerIndex = 0;
      for (int group = 0; group < groupCount; group++) {
         int n = abundances.length / groupCount;
         if (group < abundances.length % groupCount) {
            n++;
         }
         double start = groupStart[group];
         double end = groupEnd[group];
         double delta = (end - start) / n;
         if (delta >= minBinSize) {
            for (int i = 0; i < n; i++) {
               dividers[dividerIndex++] = start;
               start += delta;
               dividers[dividerIndex++] = start;
            }
         } else {
            ok = false;
            break;
         }
      }

      if (ok) {
         assert dividerIndex == dividers.length;
         sizeHistogram.computeCenters();
      } else {
         sizeHistogram.copyDividers(dividerBackup);
      }

      return ok;
   }
}
