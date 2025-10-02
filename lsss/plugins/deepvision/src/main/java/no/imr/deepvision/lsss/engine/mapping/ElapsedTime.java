package no.imr.deepvision.lsss.engine.mapping;

import org.jspecify.annotations.Nullable;

final class ElapsedTime {
   private long elapsedTime;
   private final long startTime;
   private final long interval;

   ElapsedTime(long startTime, long interval) {
      this.startTime = startTime;
      this.interval = interval;
   }

   static ElapsedTime checkElapsedTime(@Nullable ElapsedTime elapsedTime, long time, int interval) {
      if (elapsedTime == null) {
         return new ElapsedTime(time, interval);
      }
      elapsedTime.set(time);
      return elapsedTime;
   }

   private void set(long time) {
      elapsedTime = time - startTime;
   }

   boolean inInterval() {
      return !(elapsedTime == 0 || elapsedTime > interval);
   }
}
