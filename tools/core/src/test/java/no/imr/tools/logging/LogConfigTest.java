package no.imr.tools.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class LogConfigTest {
   @Test
   void test() {
      assertTrue(LogConfig.isDone(), "Run with -Djava.util.logging.config.class=no.imr.tools.logging.LogConfig");
      assertEquals("no.imr.tools.logging.LogConfig", System.getProperty("java.util.logging.config.class"));
   }
}
