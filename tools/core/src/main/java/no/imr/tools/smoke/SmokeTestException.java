package no.imr.tools.smoke;

/**
 * A smoke test should throw this exception when an error condition is detected.
 */
public final class SmokeTestException extends RuntimeException {
   public SmokeTestException(String message) {
      super(message);
   }

   public SmokeTestException(String message, Throwable cause) {
      super(message, cause);
   }
}
