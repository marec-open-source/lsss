package no.imr.deepvision.lsss.engine.mapping;

import no.imr.tools.time.TimeUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

final class TimeInterpolator {
   private final Instant[] deepVisionTimes;
   private final Instant[] lsssTimes;

   TimeInterpolator(Instant[] deepVisionTimes, Instant[] lsssTimes) {
      this.deepVisionTimes = deepVisionTimes;
      this.lsssTimes = lsssTimes;
   }

   Instant lsssTimeToDeepVisionTime(Instant lsssTime) {
      return timeToOtherTime(lsssTime, lsssTimes, deepVisionTimes);
   }

   Instant deepVisionTimeToLsssTime(Instant deepVisionTime) {
      return timeToOtherTime(deepVisionTime, deepVisionTimes, lsssTimes);
   }

   private static Instant timeToOtherTime(Instant time, Instant[] times, Instant[] otherTimes) {
      int i = Arrays.binarySearch(times, time);
      if (i >= 0) {
         // Exact match.
         return otherTimes[i];
      }
      i = -(i + 1); // Conversion to insertion point.
      if (i <= 0 || i >= times.length) {
         return time;
      }
      Instant timeA = times[i - 1];
      Instant timeB = times[i];
      Instant otherTimeA = otherTimes[i - 1];
      Instant otherTimeB = otherTimes[i];
      double w = (double) timeA.until(time, ChronoUnit.NANOS) / timeA.until(timeB, ChronoUnit.NANOS);
      return TimeUtils.interpolateInstant(otherTimeA, otherTimeB, w);
   }
}
