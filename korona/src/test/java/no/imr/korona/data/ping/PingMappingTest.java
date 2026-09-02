package no.imr.korona.data.ping;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

final class PingMappingTest {
   @Test
   void timeValue() {
      Instant instant = Instant.ofEpochSecond(1770801161, 987654321);
      DefaultPingIndex a = new DefaultPingIndex(instant, 0, 0, null);
      assertEquals(1770801161.98765432, PingMapping.TIME.valueOf(a));
      assertEquals(1770801161.98765432, PingMapping.instantToTimeValue(instant));

      DefaultPingIndex b = new DefaultPingIndex(Instant.ofEpochSecond(1770801161), 0, 0, null);
      assertEquals(0.987654321, PingMapping.TIME.distance(b, a));

      DefaultPingIndex c = new DefaultPingIndex(Instant.ofEpochSecond(1770801161, 1), 0, 0, null);
      assertEquals(1e-9, PingMapping.TIME.distance(b, c));

      DefaultPingIndex d = new DefaultPingIndex(c.getInstant().plus(300 * 365, ChronoUnit.DAYS), 0, 0, null);
      assertThrows(ArithmeticException.class, () -> c.getInstant().until(d.getInstant(), ChronoUnit.NANOS));
      assertEquals(300 * 365 * 24 * 3600L, PingMapping.TIME.distance(c, d));
   }
}
