package no.imr.tools.time;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

/**
 * Estimates the time to complete an amount of work.
 */
public final class RemainingTimeEstimator {
   private final Instant startTime = Instant.now();
   private Instant lastUpdateTime = startTime;
   private @Nullable Instant estimatedEndTime;
   private double remainingWork;
   private double workPerSecond;

   public RemainingTimeEstimator(double initialRemainingWork) {
      remainingWork = initialRemainingWork;
   }

   public void setRemainingWork(double newRemainingWork) {
      Instant now = Instant.now();
      double seconds = TimeUtils.toSeconds(lastUpdateTime, now);
      if (seconds < 1) {
         return;
      }

      double workDone = remainingWork - newRemainingWork;
      if (workDone <= 0) {
         return;
      }

      lastUpdateTime = now;
      remainingWork = newRemainingWork;

      double newWorkPerSecond = workDone / seconds;
      if (workPerSecond == 0) {
         workPerSecond = newWorkPerSecond;
      } else {
         double alpha = Math.min(1, seconds / Math.min(TimeUtils.toSeconds(startTime, now), 60));
         workPerSecond += alpha * (newWorkPerSecond - workPerSecond);
      }
      double remainingSeconds = Math.min(remainingWork / workPerSecond, 365 * 24 * 3600);
      estimatedEndTime = now.plusSeconds((long) Math.ceil(remainingSeconds));
   }

   public String getRemainingTimeString() {
      if (estimatedEndTime == null) {
         return "?";
      }
      Duration remaining = Instant.now().until(estimatedEndTime);
      if (remaining.isNegative()) {
         remaining = Duration.ZERO;
      }
      return TimeUtils.getDurationString(remaining);
   }

   public String getTotalTimeString() {
      if (estimatedEndTime == null) {
         return "?";
      }
      return TimeUtils.getDurationString(startTime.until(estimatedEndTime));
   }

   public String getElapsedTimeString() {
      return TimeUtils.getDurationString(startTime.until(Instant.now()));
   }

   public String getHtmlTable() {
      return "<table cellpadding=0 cellspacing=0>"
            + "<tr><td>Elapsed:<td>" + getElapsedTimeString()
            + "<tr><td>Remaining:&nbsp;<td>" + getRemainingTimeString()
            + "<tr><td>Total:&nbsp;<td>" + getTotalTimeString()
            + "</table>";
   }
}
