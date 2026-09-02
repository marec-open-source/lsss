package no.imr.tools.time;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;

public final class TimeFractionCalculator {
   private final int bufferSize;
   private final Deque<Observation> observations = new ArrayDeque<>();
   private long durationNanosSum = 0;

   public TimeFractionCalculator(int bufferSize) {
      this.bufferSize = bufferSize;
   }

   public void addDuration(Instant beginTime) {
      Observation observation = new Observation(beginTime, beginTime.until(Instant.now(), ChronoUnit.NANOS));
      observations.add(observation);
      durationNanosSum += observation.durationNanos;
      if (observations.size() > bufferSize) {
         durationNanosSum -= observations.removeFirst().durationNanos;
      }
   }

   public double calculateDurationFraction() {
      if (observations.isEmpty()) {
         return 0;
      }
      long totalNanos = observations.getFirst().beginTime.until(Instant.now(), ChronoUnit.NANOS);
      if (totalNanos == 0) {
         return 0;
      }
      return (double) durationNanosSum / totalNanos;
   }

   private record Observation(Instant beginTime, long durationNanos) {
   }
}
