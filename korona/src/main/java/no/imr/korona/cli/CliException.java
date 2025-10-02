package no.imr.korona.cli;

import no.imr.tools.NoCanDoException;

public final class CliException extends NoCanDoException {
   public CliException(String message) {
      super(message);
   }

   public CliException(String message, Throwable cause) {
      super(message, cause);
   }
}
