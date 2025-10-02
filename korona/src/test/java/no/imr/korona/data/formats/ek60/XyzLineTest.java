package no.imr.korona.data.formats.ek60;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

final class XyzLineTest {
   @Test
   void testFormatVer1() {
      XyzLine xyzLine = new XyzLine("6211.5247932 -0673.6011854 4.80 11122020 085514.98 -0.062");
      assertEquals(62.115247932, xyzLine.latitude);
      assertEquals(-6.736011854, xyzLine.longitude);
      assertEquals(4.8f, xyzLine.depth);
      assertEquals(ZonedDateTime.of(2020, 12, 11, 8, 55, 14, 980_000_000, ZoneOffset.UTC).toInstant().toEpochMilli(),
            xyzLine.timeInMillis);
      assertEquals(-0.062f, xyzLine.transducerOffset);
   }

   @Test
   void testFormatVer2() {
      XyzLine xyzLine = new XyzLine("62.115247932 -06.736011854 4.80 11122020 085514.98 -0.062");
      assertEquals(62.115247932, xyzLine.latitude);
      assertEquals(-6.736011854, xyzLine.longitude);
      assertEquals(4.8f, xyzLine.depth);
      assertEquals(ZonedDateTime.of(2020, 12, 11, 8, 55, 14, 980_000_000, ZoneOffset.UTC).toInstant().toEpochMilli(),
            xyzLine.timeInMillis);
      assertEquals(-0.062f, xyzLine.transducerOffset);
   }

   @Test
   void testFormatVer3() {
      XyzLine xyzLine = new XyzLine("38.0000211 N 00.0006240 E 79.62 17122020 182440.51 5.000");
      assertEquals(38.0000211, xyzLine.latitude);
      assertEquals(0.0006240, xyzLine.longitude);
      assertEquals(79.62f, xyzLine.depth);
      assertEquals(ZonedDateTime.of(2020, 12, 17, 18, 24, 40, 510_000_000, ZoneOffset.UTC).toInstant().toEpochMilli(),
            xyzLine.timeInMillis);
      assertEquals(5, xyzLine.transducerOffset);

      xyzLine = new XyzLine("38.0000211 S 00.0006240 W 79.62 17122020 182440.51 5.000");
      assertEquals(-38.0000211, xyzLine.latitude);
      assertEquals(-0.0006240, xyzLine.longitude);
   }
}
