package no.imr.tools.time;

import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Synchronization with real time.
 */
public final class RealtimeSyncer {
   private Instant lastRealTime = Instant.now();
   private @Nullable Instant lastSyncTime;

   public RealtimeSyncer() {
   }

   public void sync(AsyncHandle asyncHandle, Instant syncTime, double realtimeFactor, boolean fastForward) {
      Duration sleepDuration = getSleepDuration(syncTime, realtimeFactor, fastForward);
      if (sleepDuration.isPositive()) {
         asyncHandle.sleep(sleepDuration.toMillis());
      }
      update(syncTime);
   }

   public Duration getSleepDuration(Instant syncTime, double realtimeFactor, boolean fastForward) {
      if (lastSyncTime == null || fastForward) {
         return Duration.ZERO;
      } else {
         long syncTimeDelta = lastSyncTime.until(syncTime, ChronoUnit.NANOS);
         // Remove very long jumps in time.
         syncTimeDelta = Math.min(syncTimeDelta, 10 * 1_000_000_000L);

         long realTimeDelta = (long) (syncTimeDelta / realtimeFactor);
         Instant nextRealTime = lastRealTime.plusNanos(realTimeDelta);
         return Instant.now().until(nextRealTime);
      }
   }

   public void update(Instant syncTime) {
      lastRealTime = Instant.now();
      lastSyncTime = syncTime;
   }
}
