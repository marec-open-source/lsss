package no.imr.deepvision.lsss.engine.mapping;

import java.util.Arrays;

final class TimeInterpolator {
   private final long[] deepVisionTimes;
   private final long[] lsssTimes;

   private enum Interpolation {
      DV_TO_LSSS, LSSS_TO_DV
   }

   TimeInterpolator(long[] deepVisionTimes, long[] lsssTimes) {
      this.deepVisionTimes = deepVisionTimes;
      this.lsssTimes = lsssTimes;
   }

   private long interpolate(long time, int i, Interpolation interpolation) {
      long dvi = deepVisionTimes[i];
      long dvip1 = deepVisionTimes[i + 1];
      long lsssi = lsssTimes[i];
      long lsssip1 = lsssTimes[i + 1];
      if (interpolation == Interpolation.LSSS_TO_DV) {
         long denominator = lsssip1 - lsssi;
         double w = (double) (time - lsssi) / denominator;
         return (long) (dvi + w * (dvip1 - dvi));
      } else {
         long denominator = dvip1 - dvi;
         double w = (double) (time - dvi) / denominator;
         return (long) (lsssi + w * (lsssip1 - lsssi));
      }
   }

   long lsssTimeToDeepVisionTime(long lsssTime) {
      int i = Arrays.binarySearch(lsssTimes, lsssTime);
      if (i < 0) {
         // not exact match
         i = -(i + 1); // conversion to insertion point
         if (i == 0) {
            return lsssTime;
         }
         i = i - 1;
         if (i >= lsssTimes.length - 1) {
            return lsssTime;
         }
         return interpolate(lsssTime, i, Interpolation.LSSS_TO_DV);
      }
      return deepVisionTimes[i];
   }

   long deepVisionTimeToLsssTime(long deepVisionTime) {
      int i = Arrays.binarySearch(deepVisionTimes, deepVisionTime);
      if (i < 0) {
         // not exact match
         i = -(i + 1); // conversion to insertion point
         if (i == 0) {
            return deepVisionTime;
         }
         i = i - 1;
         if (i >= deepVisionTimes.length - 1) {
            return deepVisionTime;
         }
         return interpolate(deepVisionTime, i, Interpolation.DV_TO_LSSS);
      }
      return lsssTimes[i];
   }
}
