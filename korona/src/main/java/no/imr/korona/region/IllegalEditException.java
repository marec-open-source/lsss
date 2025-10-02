package no.imr.korona.region;

import no.imr.tools.NoCanDoException;

public final class IllegalEditException extends NoCanDoException {
   public IllegalEditException() {
      super("Illegal edit");
   }
}
