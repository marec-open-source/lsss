package no.imr.tools;

public class NoCanDoException extends RuntimeException {
   public NoCanDoException(String message) {
      super(message);
   }

   public NoCanDoException(String message, Throwable cause) {
      super(message, cause);
   }
}
