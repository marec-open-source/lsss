package no.imr.korona.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class KoronaUtilsTest {
   @Test
   void knotsToMeterPerSecond() {
      assertEquals(3.45, KoronaUtils.knotsToMeterPerSecond(KoronaUtils.meterPerSecondToKnots(3.45)), 1e-6);
   }

   @Test
   void fromDB() {
      assertEquals(3.45, KoronaUtils.fromDB(KoronaUtils.toDB(3.45)), 1e-6);
   }
}
