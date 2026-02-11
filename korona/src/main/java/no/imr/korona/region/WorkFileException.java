package no.imr.korona.region;

public final class WorkFileException extends Exception {
   public WorkFileException(String message) {
      super(message);
   }

   public WorkFileException(String message, Throwable cause) {
      super(message, cause);
   }
}
