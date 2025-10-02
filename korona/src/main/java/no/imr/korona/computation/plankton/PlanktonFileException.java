package no.imr.korona.computation.plankton;

import java.io.IOException;

/**
 * Exception for errors in plankton file format.
 */
public final class PlanktonFileException extends IOException {
   public PlanktonFileException(String msg) {
      super(msg);
   }
}
