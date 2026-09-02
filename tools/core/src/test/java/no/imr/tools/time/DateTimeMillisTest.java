package no.imr.tools.time;

import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

final class DateTimeMillisTest {
   @Test
   void date() {
      checkDate("", 0, Optional.empty());
      checkDate("0001-01-01", 1_01_01, Optional.of(LocalDate.of(1, 1, 1)));
      checkDate("2026-03-06", 2026_03_06, Optional.of(LocalDate.of(2026, 3, 6)));
      checkDate("9999-12-31", 9999_12_31, Optional.of(LocalDate.of(9999, 12, 31)));
   }

   private static void checkDate(String s, int date, Optional<LocalDate> localDate) {
      assertEquals(localDate, DateTimeMillis.toLocalDate(s));
      assertEquals(s, DateTimeMillis.localDateToString(localDate));

      assertEquals(localDate, DateTimeMillis.toLocalDate(date));
      assertEquals(date, DateTimeMillis.localDateToInt(localDate));
   }

   @Test
   void time() {
      checkTime(0, LocalTime.of(0, 0, 0));
      checkTime(1_01_01_000, LocalTime.of(1, 1, 1));
      checkTime(13_08_07_025, LocalTime.of(13, 8, 7, 25_000_000));
      checkTime(23_59_59_999, LocalTime.of(23, 59, 59, 999_000_000));
   }

   private static void checkTime(int time, LocalTime localTime) {
      assertEquals(localTime, DateTimeMillis.toLocalTime(time));
      assertEquals(time, DateTimeMillis.localTimeToInt(localTime));
   }

   @Test
   void dateTime() {
      int date = 2005_11_28;
      int time = 14_27_01_343;
      Instant instant = LocalDate.of(2005, 11, 28).atTime(
            14, 27, 1, 343_000_000).toInstant(ZoneOffset.UTC);

      assertEquals(instant, DateTimeMillis.toInstant(date, time));

      DateTimeMillis t1 = new DateTimeMillis(instant);
      assertEquals(date, t1.getDate());
      assertEquals(time, t1.getTime());
   }

   @Test
   void centisTime() {
      assertEquals(LocalTime.of(0, 0), DateTimeMillis.centisTimeToLocalTime(""));
      assertEquals(LocalTime.of(0, 0), DateTimeMillis.centisTimeToLocalTime("0"));
      assertEquals(LocalTime.of(1, 22), DateTimeMillis.centisTimeToLocalTime("1:22"));

      checkCentisTime(0, "00:00", LocalTime.of(0, 0));
      checkCentisTime(9_00_00_00, "09:00", LocalTime.of(9, 0, 0));
      checkCentisTime(9_08_00_00, "09:08", LocalTime.of(9, 8, 0));
      checkCentisTime(9_08_07_00, "09:08:07", LocalTime.of(9, 8, 7));
      checkCentisTime(9_08_07_06, "09:08:07:06", LocalTime.of(9, 8, 7, 60_000_000));
      checkCentisTime(9_08_00_06, "09:08:00:06", LocalTime.of(9, 8, 0, 60_000_000));

      assertThrows(DateTimeException.class, () -> DateTimeMillis.centisIntToLocalTime(1_22_60_00));
      assertThrows(DateTimeException.class, () -> DateTimeMillis.centisTimeToLocalTime("1:22:60"));
   }

   private static void checkCentisTime(int centisTime, String s, LocalTime localTime) {
      assertEquals(centisTime, DateTimeMillis.localTimeToCentisInt(localTime));
      assertEquals(localTime, DateTimeMillis.centisIntToLocalTime(centisTime));

      assertEquals(s, DateTimeMillis.localTimeToCentisString(localTime));
      assertEquals(localTime, DateTimeMillis.centisTimeToLocalTime(s));
   }
}
