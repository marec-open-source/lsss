package no.imr.tools.time;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

final class TimeUtilsTest {
   @Test
   void getDurationString() {
      assertEquals("0:00:00", TimeUtils.getDurationString(Duration.ZERO));
      assertEquals("0:00:00", TimeUtils.getDurationString(Duration.ofNanos(999_999_999)));
      assertEquals("1:01:01", TimeUtils.getDurationString(Duration.ofSeconds(3661)));
      assertEquals("100:00:59", TimeUtils.getDurationString(Duration.ofSeconds(3600 * 100 + 59)));
   }

   @Test
   void toSecondsInstantToInstant() {
      Instant instant = Instant.ofEpochSecond(1770801161, 987654321);
      assertEquals(3.000_000_001, TimeUtils.toSeconds(instant, instant.plusSeconds(3).plusNanos(1)));
      assertEquals(2.999_999_999, TimeUtils.toSeconds(instant, instant.plusSeconds(3).plusNanos(-1)));
      assertEquals(-2.999_999_999, TimeUtils.toSeconds(instant, instant.plusSeconds(-3).plusNanos(1)));
      assertEquals(-3.000_000_001, TimeUtils.toSeconds(instant, instant.plusSeconds(-3).plusNanos(-1)));
   }

   @Test
   void toSecondsDuration() {
      assertEquals(3.000_000_001, TimeUtils.toSeconds(Duration.ofSeconds(3, 1)));
   }

   @Test
   void secondsToDuration() {
      assertEquals(Duration.ofSeconds(3, 1), TimeUtils.secondsToDuration(3.000_000_001));
      assertEquals(Duration.ofSeconds(2, 999_999_999), TimeUtils.secondsToDuration(2.999_999_999));
      assertEquals(Duration.ofSeconds(-2, -999_999_999), TimeUtils.secondsToDuration(-2.999_999_999));
      assertEquals(Duration.ofSeconds(-3, -1), TimeUtils.secondsToDuration(-3.000_000_001));
   }

   @Test
   void roundedTo() {
      Instant instant = Instant.parse("2026-02-12T09:00:00Z");

      assertEquals(instant, TimeUtils.roundedTo(instant.plusNanos(499_999), ChronoUnit.MILLIS));
      assertEquals(instant.plusMillis(1), TimeUtils.roundedTo(instant.plusNanos(500_000), ChronoUnit.MILLIS));

      assertEquals(instant, TimeUtils.roundedTo(instant.plusMillis(499), ChronoUnit.SECONDS));
      assertEquals(instant.plusSeconds(1), TimeUtils.roundedTo(instant.plusMillis(500), ChronoUnit.SECONDS));

      assertEquals(instant, TimeUtils.roundedTo(instant.plusSeconds(29), ChronoUnit.MINUTES));
      assertEquals(instant.plusSeconds(60), TimeUtils.roundedTo(instant.plusSeconds(30), ChronoUnit.MINUTES));

      assertEquals(instant, TimeUtils.roundedTo(instant.plusSeconds(29 * 60), ChronoUnit.HOURS));
      assertEquals(instant.plusSeconds(60 * 60), TimeUtils.roundedTo(instant.plusSeconds(30 * 60), ChronoUnit.HOURS));
   }

   @Test
   void ceiledTo() {
      Instant a = Instant.parse("2026-02-12T09:00:00Z");
      Instant b = a.plusNanos(1);

      assertEquals(a, TimeUtils.ceiledTo(a, ChronoUnit.MILLIS));
      assertEquals(a.plusMillis(1), TimeUtils.ceiledTo(b, ChronoUnit.MILLIS));

      assertEquals(a, TimeUtils.ceiledTo(a, ChronoUnit.SECONDS));
      assertEquals(a.plusSeconds(1), TimeUtils.ceiledTo(b, ChronoUnit.SECONDS));

      assertEquals(a, TimeUtils.ceiledTo(a, ChronoUnit.MINUTES));
      assertEquals(a.plusSeconds(60), TimeUtils.ceiledTo(b, ChronoUnit.MINUTES));

      assertEquals(a, TimeUtils.ceiledTo(a, ChronoUnit.HOURS));
      assertEquals(a.plusSeconds(60 * 60), TimeUtils.ceiledTo(b, ChronoUnit.HOURS));
   }

   @Test
   void interpolateInstant() {
      Instant a = Instant.parse("2026-02-12T09:00:00Z");
      Instant b = a.plusSeconds(10);
      assertEquals(a.plusSeconds(-10), TimeUtils.interpolateInstant(a, b, -1));
      assertEquals(a, TimeUtils.interpolateInstant(a, b, 0));
      assertEquals(a.plusSeconds(1), TimeUtils.interpolateInstant(a, b, 0.1));
      assertEquals(a.plusMillis(1_200), TimeUtils.interpolateInstant(a, b, 0.12));
      assertEquals(a.plusNanos(1_234_567_891), TimeUtils.interpolateInstant(a, b, 0.1234567891));
      assertEquals(a.plusSeconds(9), TimeUtils.interpolateInstant(a, b, 0.9));
      assertEquals(b, TimeUtils.interpolateInstant(a, b, 1));
      assertEquals(b.plusSeconds(10), TimeUtils.interpolateInstant(a, b, 2));
   }
}
