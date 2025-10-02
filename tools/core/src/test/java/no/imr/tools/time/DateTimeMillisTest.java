package no.imr.tools.time;

import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

final class DateTimeMillisTest {
   @Test
   void testDateTime() {
      int date = 2005_11_28;
      int time = 14_27_01_343;

      long millis = DateTimeMillis.toMillis(date, time);
      DateTimeMillis t1 = new DateTimeMillis(millis);

      assertEquals(date, t1.getDate());
      assertEquals(time, t1.getTime());
   }

   @Test
   void testMillis() {
      long millis = 1133185231173L;
      DateTimeMillis t0 = new DateTimeMillis(millis);

      int date = t0.getDate();
      int time = t0.getTime();

      assertEquals(millis, DateTimeMillis.toMillis(date, time));
   }

   @Test
   void dateString() {
      assertEquals(0, DateTimeMillis.stringDateToInt(""));
      assertEquals("", DateTimeMillis.intDateToString(0));

      assertEquals(2010_11_22, DateTimeMillis.stringDateToInt("2010-11-22"));
      assertEquals("2010-11-22", DateTimeMillis.intDateToString(2010_11_22));

      assertEquals(Optional.empty(), DateTimeMillis.toLocalDate(""));
      assertEquals("", DateTimeMillis.localDateToString(Optional.empty()));
   }

   @Test
   void invalidDateAsInt() {
      assertThrows(DateTimeException.class, () -> {
         DateTimeMillis.stringDateToInt("2010-04-31");
      });
   }

   @Test
   void timeString() {
      assertEquals(0, DateTimeMillis.centisTimeToInt(""));
      assertEquals(0, DateTimeMillis.centisTimeToInt("0"));
      assertEquals(0, DateTimeMillis.centisTimeToInt("0:00"));
      assertEquals("00:00", DateTimeMillis.centisTimeToString(0));

      assertEquals(1_22_00_00, DateTimeMillis.centisTimeToInt("1:22"));
      assertEquals(1_22_00_00, DateTimeMillis.centisTimeToInt("01:22"));
      assertEquals("01:22", DateTimeMillis.centisTimeToString(1_22_00_00));

      assertEquals(1_22_33_00, DateTimeMillis.centisTimeToInt("1:22:33"));
      assertEquals("01:22:33", DateTimeMillis.centisTimeToString(1_22_33_00));

      assertEquals(1_22_33_44, DateTimeMillis.centisTimeToInt("1:22:33:44"));
      assertEquals("01:22:33:44", DateTimeMillis.centisTimeToString(1_22_33_44));

      assertEquals(Optional.empty(), DateTimeMillis.centisTimeToLocalTime(""));
      assertEquals("", DateTimeMillis.localTimeToCentisString(Optional.empty()));
   }

   @Test
   void invalidTimeAsInt() {
      assertThrows(DateTimeException.class, () -> {
         DateTimeMillis.stringDateToInt("1:22:60");
      });
   }
}
