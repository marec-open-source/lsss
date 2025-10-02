package no.imr.tools.time;

import no.imr.tools.Utils;

/**
 * Synchronization with real time.
 */
public final class RealtimeSyncer {
   private long lastRealTime;
   private long lastSyncTime;

   public RealtimeSyncer() {
   }

   public void sync(long syncTimeInMillis, double realtimeFactor, boolean fastForward) {
      long sleepTime = getSleepTime(syncTimeInMillis, realtimeFactor, fastForward);
      if (sleepTime > 0) {
         Utils.sleep(sleepTime);
      }
      update(syncTimeInMillis);
   }

   public long getSleepTime(long syncTimeInMillis, double realtimeFactor, boolean fastForward) {
      if (lastSyncTime == 0 || fastForward) {
         return 0;
      } else {
         long syncTimeDelta = syncTimeInMillis - lastSyncTime;
         // Remove very long jumps in time
         syncTimeDelta = Math.min(syncTimeDelta, 10 * 1000);

         long realTimeDelta = (long) (syncTimeDelta / realtimeFactor);
         long nextRealTime = lastRealTime + realTimeDelta;
         return nextRealTime - System.currentTimeMillis();
      }
   }

   public void update(long syncTimeInMillis) {
      lastRealTime = System.currentTimeMillis();
      lastSyncTime = syncTimeInMillis;
   }
}
