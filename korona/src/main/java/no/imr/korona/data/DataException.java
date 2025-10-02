package no.imr.korona.data;

import java.io.IOException;

/**
 * Thrown when a data segment could not be created.
 */
public class DataException extends IOException {
   /**
    * Creates a new DataException.
    *
    * @param message description of error
    */
   public DataException(String message) {
      super(message);
   }

   /**
    * Creates a new DataException.
    *
    * @param message description of error
    * @param cause   cause of error
    */
   public DataException(String message, Throwable cause) {
      super(message, cause);
   }
}
