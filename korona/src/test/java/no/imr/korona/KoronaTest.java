package no.imr.korona;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class KoronaTest {
   @Test
   void version() {
      assertFalse(Korona.VERSION.startsWith("@"));
   }
}
