package no.imr.korona.region;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.DataUtils;
import no.imr.tools.Max;
import no.imr.tools.Min;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.RangeSet;

import java.util.Arrays;

public final class ExclusionDetector {
   private ExclusionDetector() {
   }

   public static RangeSet<PingIndex> fromLowSpeed(PingContainer pingContainer, PingRange pingRange, double minKnots) {
      double[] knots = pingContainer.getPingIndexStream(pingRange)
            .mapToDouble(pingIndex -> DataUtils.getKnotsByVesselDistance(pingIndex, pingContainer))
            .toArray();
      boolean[] excluded = new boolean[knots.length];

      double maxKnotsDiff = 0.1;
      int step = 10;

      for (int i = 0; i < knots.length; i++) {
         if (knots[i] < minKnots) {
            excluded[i] = true;
            if (i > 0 && knots[i - 1] >= minKnots) {
               // This is the first ping of a low speed interval.
               int j = i;
               while (j > 0 && diff(knots, j, -step) > maxKnotsDiff) {
                  j--;
               }
               Arrays.fill(excluded, j, i, true);
            }
         } else {
            if (i > 0 && knots[i - 1] < minKnots) {
               // This is the first ping after a low speed interval.
               int j = i;
               while (j < knots.length && diff(knots, j, step) > maxKnotsDiff) {
                  j++;
               }
               Arrays.fill(excluded, i, j, true);
            }
         }
      }

      RangeSet<PingIndex> result = new ArrayRangeSet<>();
      long beginPingNumber = pingRange.begin().getPingNumber();
      for (int i = 0; i < excluded.length; i++) {
         if (excluded[i]) {
            PingIndex begin = pingContainer.getPingIndex(beginPingNumber + i);
            while (i < excluded.length && excluded[i]) {
               i++;
            }
            PingIndex end = pingContainer.getPingIndex(beginPingNumber + i);
            result.add(begin, end);
         }
      }
      return result;
   }

   private static double diff(double[] array, int i, int step) {
      double a = getClamped(array, i);
      double b = getClamped(array, i + step);
      double c = getClamped(array, i + 2 * step);
      return Max.of(a, b, c) - Min.of(a, b, c);
   }

   private static double getClamped(double[] array, int i) {
      return array[Math.clamp(i, 0, array.length - 1)];
   }
}
