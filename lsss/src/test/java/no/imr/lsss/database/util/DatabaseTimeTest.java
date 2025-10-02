package no.imr.lsss.database.util;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

final class DatabaseTimeTest {
   @Test
   void testDateTime() {
      int date = 2005_11_28;
      int time = 14_27_01_34;

      long millis = DatabaseTime.toMillis(date, time);
      DatabaseTime t1 = new DatabaseTime(millis);

      assertEquals(millis, t1.getMillis());
      assertEquals(date, t1.getDate());
      assertEquals(time, t1.getTime());
   }

   @Test
   void testMillis() {
      long millis = 1133185231170L;
      DatabaseTime t0 = new DatabaseTime(millis);
      assertEquals(millis, t0.getMillis());

      int date = t0.getDate();
      int time = t0.getTime();

      assertEquals(millis, DatabaseTime.toMillis(date, time));
   }

   @Test
   void testUTC() {
      long millis = ZonedDateTime.of(2006, 12, 25, 14, 33, 22, 0, ZoneId.of("UTC+7")).toEpochSecond() * 1000;
      DatabaseTime databaseTime = new DatabaseTime(millis);
      assertEquals(7_33_22_00, databaseTime.getTime());
   }
}
