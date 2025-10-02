package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.DataException;

/**
 * Thrown when a raw file does not have an idx file.
 */
public final class MissingIdxFileException extends DataException {
   public MissingIdxFileException(String message) {
      super(message);
   }
}
