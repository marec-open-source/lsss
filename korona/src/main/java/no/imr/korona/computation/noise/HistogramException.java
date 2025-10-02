package no.imr.korona.computation.noise;

/**
 * Thrown when a histogram operation fails.
 */
public final class HistogramException extends Exception {
   public HistogramException(String message) {
      super(message);
   }

   public HistogramException(String message, Throwable cause) {
      super(message, cause);
   }

   public HistogramException(Throwable cause) {
      super(cause);
   }
}
