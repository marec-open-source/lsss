package no.imr.tools.time;

import java.util.ArrayDeque;
import java.util.Deque;

public final class TimeFractionCalculator {
   private final int bufferSize;
   private final Deque<Observation> observations = new ArrayDeque<>();
   private long durationSum = 0;

   public TimeFractionCalculator(int bufferSize) {
      this.bufferSize = bufferSize;
   }

   public void addDuration(long beginNanos) {
      Observation observation = new Observation(beginNanos, System.nanoTime() - beginNanos);
      observations.add(observation);
      durationSum += observation.duration;
      if (observations.size() > bufferSize) {
         durationSum -= observations.removeFirst().duration;
      }
   }

   public double calculateDurationFraction() {
      if (observations.isEmpty()) {
         return 0;
      }
      return (double) durationSum / (double) (System.nanoTime() - observations.getFirst().beginNanos);
   }

   private record Observation(long beginNanos, long duration) {
   }
}
