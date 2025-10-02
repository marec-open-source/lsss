package no.imr.lsss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class LsssTest {
   @Test
   void version() {
      assertFalse(LSSS.VERSION.startsWith("@"));
   }
}
