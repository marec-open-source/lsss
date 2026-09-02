package no.imr.tools.time;

import java.time.Instant;

/**
 * Time in units of hundred nanoseconds since 1601-01-01T00:00:00Z.
 */
public final class NTDate {
   static final long NT_DATE_ZERO_AS_EPOCH_SECOND = -11644473600L;

   public static final int NANOSECONDS_PER_NT_DATE_UNIT = 100;
   private static final int NT_DATE_UNITS_PER_SECOND = 10_000_000;

   private NTDate() {
   }

   public static Instant ntDateToInstant(long ntDate) {
      long secs = Math.floorDiv(ntDate, NT_DATE_UNITS_PER_SECOND) + NT_DATE_ZERO_AS_EPOCH_SECOND;
      int nanos = Math.floorMod(ntDate, NT_DATE_UNITS_PER_SECOND) * NANOSECONDS_PER_NT_DATE_UNIT;
      return Instant.ofEpochSecond(secs, nanos);
   }

   public static long instantToNTDate(Instant instant) {
      return (instant.getEpochSecond() - NT_DATE_ZERO_AS_EPOCH_SECOND) * NT_DATE_UNITS_PER_SECOND
            + instant.getNano() / NANOSECONDS_PER_NT_DATE_UNIT;
   }

   public static String instantToNTDateString(Instant instant) {
      long ntDate = instantToNTDate(instant);
      int excess = instant.getNano() % NANOSECONDS_PER_NT_DATE_UNIT;
      if (excess == 0) {
         return Long.toString(ntDate);
      }
      if (ntDate < 0) {
         ntDate++;
         excess = 100 - excess;
         if (ntDate == 0) {
            return Float.toString(-excess / 100f);
         }
      }
      if (excess % 10 == 0) {
         return ntDate + "." + excess / 10;
      } else if (excess < 10) {
         return ntDate + ".0" + excess;
      } else {
         return ntDate + "." + excess;
      }
   }

   public static Instant ntDateStringToInstant(String ntDateString) {
      int i = ntDateString.indexOf('.');
      if (i < 0) {
         long ntDate = Long.parseLong(ntDateString);
         return ntDateToInstant(ntDate);
      } else {
         long ntDate = Long.parseLong(ntDateString, 0, i, 10);
         i++; // Skip '.'.
         int excess = switch (ntDateString.length() - i) {
            case 0 -> 0;
            case 1 -> 10 * Integer.parseInt(ntDateString, i, i + 1, 10);
            default -> Integer.parseInt(ntDateString, i, i + 2, 10);
         };
         if (ntDateString.charAt(0) == '-') {
            excess = -excess;
         }
         return ntDateToInstant(ntDate).plusNanos(excess);
      }
   }
}
