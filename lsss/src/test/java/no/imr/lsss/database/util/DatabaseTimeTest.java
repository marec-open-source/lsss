package no.imr.lsss.database.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

final class DatabaseTimeTest {
   @Test
   void testDateTime() {
      int date = 2005_11_28;
      int time = 14_27_01_34;

      Instant instant = DatabaseTime.toInstant(date, time);
      DatabaseTime t1 = new DatabaseTime(instant);

      assertEquals(instant, t1.getInstant());
      assertEquals(date, t1.getDate());
      assertEquals(time, t1.getTime());
   }

   @Test
   void testUTC() {
      Instant instant = LocalDate.of(2006, 12, 25).atTime(14, 33, 22).toInstant(ZoneOffset.ofHours(7));
      DatabaseTime databaseTime = new DatabaseTime(instant);
      assertEquals(2006_12_25, databaseTime.getDate());
      assertEquals(7_33_22_00, databaseTime.getTime());
   }

   @Test
   void truncatedInstant() {
      Instant time = Instant.ofEpochSecond(123456789);
      assertSame(time, DatabaseTime.truncatedInstant(time));
      assertEquals(time, DatabaseTime.truncatedInstant(time.plusNanos(1)));
      assertEquals(time.plusMillis(990), DatabaseTime.truncatedInstant(time.plusNanos(999_999_999)));
   }
}
