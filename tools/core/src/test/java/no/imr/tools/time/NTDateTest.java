package no.imr.tools.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

final class NTDateTest {
   @Test
   void ntDateToTimeInMillis() {
      long ntStartDate = LocalDate.of(1601, Month.JANUARY, 1).atStartOfDay(ZoneOffset.UTC).toEpochSecond() * 1000;
      assertEquals(0, NTDate.timeInMillisToNTDate(ntStartDate));
      assertEquals(ntStartDate, NTDate.ntDateToTimeInMillis(0));
   }

   @Test
   void ntDateToInstant() {
      Instant ntStartDate = LocalDate.of(1601, Month.JANUARY, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
      checkNTDate(ntStartDate, 0);

      checkNTDate(ntStartDate.plus(100, ChronoUnit.NANOS), 1);
      checkNTDate(ntStartDate.plus(987654321L * 100, ChronoUnit.NANOS), 987654321);

      checkNTDate(ntStartDate.minus(100, ChronoUnit.NANOS), -1);
      checkNTDate(ntStartDate.minus(987654321L * 100, ChronoUnit.NANOS), -987654321);

      checkNTDate(Instant.ofEpochMilli(0), NTDate.timeInMillisToNTDate(0));
      checkNTDate(Instant.ofEpochMilli(123456789), NTDate.timeInMillisToNTDate(123456789));
   }

   private static void checkNTDate(Instant instant, long ntDate) {
      assertEquals(instant, NTDate.ntDateToInstant(ntDate));
      assertEquals(ntDate, NTDate.instantToNTDate(instant));
   }
}
