package no.imr.deepvision.lsss.engine.mapping;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

final class ElapsedTime {
   private final Instant startTime;
   private long elapsedTimeNanos;
   private final long intervalNanos;

   ElapsedTime(Instant startTime, Duration interval) {
      this.startTime = startTime;
      intervalNanos = interval.toNanos();
   }

   static ElapsedTime checkElapsedTime(@Nullable ElapsedTime elapsedTime, Instant time, Duration interval) {
      if (elapsedTime == null) {
         return new ElapsedTime(time, interval);
      }
      elapsedTime.set(time);
      return elapsedTime;
   }

   private void set(Instant time) {
      elapsedTimeNanos = startTime.until(time, ChronoUnit.NANOS);
   }

   boolean inInterval() {
      return !(elapsedTimeNanos == 0 || elapsedTimeNanos > intervalNanos);
   }
}
