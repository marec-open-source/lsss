package no.imr.korona.data.ping;

import no.imr.tools.time.NTDate;
import org.jspecify.annotations.Nullable;

/**
 * For observing buffering of pings.
 */
public final class PingBuffering {
   private long ntDateIn;
   private long ntDateOut;
   private int countIn;
   private int countOut;
   private int maxBufferedCount;
   private long maxBufferedNTDate;

   public PingBuffering() {
   }

   public void in(@Nullable Ping ping) {
      if (ping != null) {
         countIn++;
         ntDateIn = ping.getNTDate();
         if (ntDateOut == 0) {
            ntDateOut = ntDateIn;
         }
         maxBufferedCount = Math.max(maxBufferedCount, countIn - countOut);
         maxBufferedNTDate = Math.max(maxBufferedNTDate, ntDateIn - ntDateOut);
      }
   }

   public void out(@Nullable Ping ping) {
      if (ping != null) {
         countOut++;
         ntDateOut = ping.getNTDate();
      }
   }

   public long getNTDateIn() {
      return ntDateIn;
   }

   public long getNTDateOut() {
      return ntDateOut;
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

   public float getBufferedSeconds() {
      return (float) (ntDateIn - ntDateOut) / (float) NTDate.UNITS_PER_SECOND;
   }

   public float getMaxBufferedSeconds() {
      return (float) maxBufferedNTDate / (float) NTDate.UNITS_PER_SECOND;
   }
}
