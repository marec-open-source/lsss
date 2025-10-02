package no.imr.tools.time;

import java.time.Instant;

/**
 * Time in units of hundred nanoseconds since 1601-01-01 00:00:00Z.
 */
public final class NTDate {
   /**
    * NT start date (1601-01-01) as milliseconds since 1970-01-01T00:00:00Z.
    */
   private static final long NT_DATE_START_AS_TIME_IN_MILLIS = -11644473600000L;

   private static final int UNITS_PER_MILLISECOND = 10000;
   public static final int UNITS_PER_SECOND = UNITS_PER_MILLISECOND * 1000;

   private NTDate() {
   }

   /**
    * Converts NT date to time in milliseconds since 1970-01-01T00:00:00Z.
    *
    * @param ntDate the NT date
    * @return milliseconds since 1970-01-01T00:00:00Z
    */
   public static long ntDateToTimeInMillis(long ntDate) {
      return ntDate / UNITS_PER_MILLISECOND + NT_DATE_START_AS_TIME_IN_MILLIS;
   }

   /**
    * Converts time in milliseconds since 1970-01-01T00:00:00Z to NT date.
    *
    * @param millis milliseconds since 1970-01-01T00:00:00Z
    * @return the NT date
    */
   public static long timeInMillisToNTDate(long millis) {
      return (millis - NT_DATE_START_AS_TIME_IN_MILLIS) * UNITS_PER_MILLISECOND;
   }

   public static Instant ntDateToInstant(long ntDate) {
      long t = ntDate + NT_DATE_START_AS_TIME_IN_MILLIS * UNITS_PER_MILLISECOND;
      long secs = Math.floorDiv(t, UNITS_PER_SECOND);
      int nanos = Math.floorMod(t, UNITS_PER_SECOND) * 100;
      return Instant.ofEpochSecond(secs, nanos);
   }

   public static long instantToNTDate(Instant instant) {
      return instant.getEpochSecond() * UNITS_PER_SECOND
            + instant.getNano() / 100
            - NT_DATE_START_AS_TIME_IN_MILLIS * UNITS_PER_MILLISECOND;
   }
}
