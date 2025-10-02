package no.imr.tools.smoke;

import no.imr.tools.logging.LoggingManager;

/**
 * Passed to {@link SmokeTestExecutor#execute(LoggingManager, SmokeTestRunnable...)} when executing a smoke test.
 */
public abstract class SmokeTestRunnable {
   protected static final String OK = "SmokeTest OK: ";

   protected SmokeTestRunnable() {
   }

   public abstract void run() throws Exception;

   protected static void checkEquals(Object expected, Object actual) {
      if (!expected.equals(actual)) {
         throw new AssertionError("Expected: " + expected + ", but was: " + actual);
      }
   }
}
