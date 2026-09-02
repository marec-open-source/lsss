package no.imr.tools.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

final class NTDateTest {
   @Test
   void ntDateToInstant() {
      Instant ntDateZero = LocalDate.of(1601, Month.JANUARY, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
      assertEquals(NTDate.NT_DATE_ZERO_AS_EPOCH_SECOND, ntDateZero.getEpochSecond());

      checkNTDate(ntDateZero, 0);

      checkNTDate(ntDateZero.plusNanos(100), 1);
      checkNTDate(ntDateZero.plusNanos(987654321L * 100), 987654321);

      checkNTDate(ntDateZero.minusNanos(100), -1);
      checkNTDate(ntDateZero.minusNanos(987654321L * 100), -987654321);

      checkNTDate(Instant.EPOCH, -NTDate.NT_DATE_ZERO_AS_EPOCH_SECOND * 10_000_000);
   }

   private static void checkNTDate(Instant instant, long ntDate) {
      assertEquals(instant, NTDate.ntDateToInstant(ntDate));
      assertEquals(ntDate, NTDate.instantToNTDate(instant));
   }

   @Test
   void ntDateString() {
      Instant instant = NTDate.ntDateToInstant(134177123475720341L);
      checkNTDateString(instant, "134177123475720341");
      assertEquals(instant, NTDate.ntDateStringToInstant("134177123475720341."));
      assertEquals(instant, NTDate.ntDateStringToInstant("134177123475720341.0"));
      assertEquals(instant, NTDate.ntDateStringToInstant("134177123475720341.00999"));
      checkNTDateString(instant.plusNanos(1), "134177123475720341.01");
      checkNTDateString(instant.plusNanos(9), "134177123475720341.09");
      checkNTDateString(instant.plusNanos(10), "134177123475720341.1");
      checkNTDateString(instant.plusNanos(99), "134177123475720341.99");
      checkNTDateString(instant.plusNanos(100), "134177123475720342");

      Instant ntDate0 = NTDate.ntDateToInstant(0);
      checkNTDateString(ntDate0.plusNanos(1), "0.01");
      checkNTDateString(ntDate0.plusNanos(10), "0.1");
      checkNTDateString(ntDate0.plusNanos(100), "1");
      checkNTDateString(ntDate0.plusNanos(101), "1.01");
      checkNTDateString(ntDate0.plusNanos(-1), "-0.01");
      checkNTDateString(ntDate0.plusNanos(-10), "-0.1");
      checkNTDateString(ntDate0.plusNanos(-100), "-1");
      checkNTDateString(ntDate0.plusNanos(-101), "-1.01");
   }

   private static void checkNTDateString(Instant instant, String ntDateString) {
      assertEquals(ntDateString, NTDate.instantToNTDateString(instant));
      assertEquals(instant, NTDate.ntDateStringToInstant(ntDateString));
   }

}
