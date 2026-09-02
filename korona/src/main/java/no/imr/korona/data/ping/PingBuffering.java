package no.imr.korona.data.ping;

import no.imr.tools.time.TimeUtils;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * For observing buffering of pings.
 */
public final class PingBuffering {
   private @Nullable Instant timeIn;
   private @Nullable Instant timeOut;
   private int countIn;
   private int countOut;
   private int maxBufferedCount;
   private double maxBufferedSeconds;

   public PingBuffering() {
   }

   public void in(@Nullable Ping ping) {
      if (ping != null) {
         countIn++;
         timeIn = ping.getInstant();
         if (timeOut == null) {
            timeOut = timeIn;
         }
         maxBufferedCount = Math.max(maxBufferedCount, countIn - countOut);
         maxBufferedSeconds = Math.max(maxBufferedSeconds, TimeUtils.toSeconds(timeOut, timeIn));
      }
   }

   public void out(@Nullable Ping ping) {
      if (ping != null) {
         countOut++;
         timeOut = ping.getInstant();
      }
   }

   public int getCountIn() {
      return countIn;
   }

   public int getCountOut() {
      return countOut;
   }

   public int getBufferedCount() {
      return countIn - countOut;
   }

   public int getMaxBufferedCount() {
      return maxBufferedCount;
   }

   public double getBufferedSeconds() {
      return timeIn != null && timeOut != null ? TimeUtils.toSeconds(timeOut, timeIn) : 0;
   }

   public double getMaxBufferedSeconds() {
      return maxBufferedSeconds;
   }
}
