package no.imr.tools.time;

import no.imr.tools.Utils;

/**
 * Estimates the time to complete an amount of work.
 */
public final class RemainingTimeEstimator {
   private final long startTime = System.currentTimeMillis();
   private long lastUpdateTime = startTime;
   private double remainingWork;
   private double workPerSeconds;

   public RemainingTimeEstimator(double initialRemainingWork) {
      remainingWork = initialRemainingWork;
   }

   public void setRemainingWork(double newRemainingWork) {
      long now = System.currentTimeMillis();
      long dt = now - lastUpdateTime;
      if (dt < 1000) {
         return;
      }

      double workDone = remainingWork - newRemainingWork;
      if (workDone <= 0) {
         return;
      }

      lastUpdateTime = now;
      remainingWork = newRemainingWork;

      double seconds = dt / 1000.0;
      double workPerSecond = workDone / seconds;
      if (workPerSeconds == 0) {
         workPerSeconds = workPerSecond;
      } else {
         double alpha = Math.min(1, dt / (double) Math.min(now - startTime, 60000));
         workPerSeconds = alpha * workPerSecond + (1 - alpha) * workPerSeconds;
      }
   }

   private double getRemainingSeconds() {
      if (remainingWork == 0) {
         return 0;
      }
      double workSeconds = remainingWork / workPerSeconds;
      double waitSeconds = (System.currentTimeMillis() - lastUpdateTime) / 1000.0;
      double remainingSeconds = workSeconds - waitSeconds;
      return Math.max(0, remainingSeconds);
   }

   public String getRemainingTimeString() {
      double remainingSeconds = getRemainingSeconds();
      if (remainingSeconds == Double.POSITIVE_INFINITY) {
         return "?";
      }
      return secondsToTimeString(remainingSeconds);
   }

   public String getTotalTimeString() {
      double remainingSeconds = getRemainingSeconds();
      if (remainingSeconds == Double.POSITIVE_INFINITY) {
         return "?";
      }
      return secondsToTimeString(remainingSeconds + getElapsedSeconds());
   }

   private double getElapsedSeconds() {
      return (System.currentTimeMillis() - startTime) / 1000.0;
   }

   public String getElapsedTimeString() {
      return secondsToTimeString(getElapsedSeconds());
   }

   public String getHtmlTable() {
      return "<table cellpadding=0 cellspacing=0><tr><td>Elapsed:<td>" + getElapsedTimeString()
            + "<tr><td>Remaining:&nbsp;<td>" + getRemainingTimeString()
            + "<tr><td>Total:&nbsp;<td>" + getTotalTimeString()
            + "</table>";
   }

   private static String secondsToTimeString(double seconds) {
      return Utils.getDurationString((long) (seconds * 1000));
   }
}
