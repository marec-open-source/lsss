package no.imr.korona.computation.tracking.impl;

import no.imr.korona.computation.tracking.Terminator;
import no.imr.korona.computation.tracking.data.Track;

public final class SimpleTerminator implements Terminator {
   private final int maxMissingPings;

   public SimpleTerminator(int maxMissingPings) {
      this.maxMissingPings = maxMissingPings;
   }

   @Override
   public boolean shouldTerminate(Track track) {
      long missingPings = track.getLastPoint().getPingIndex().getPingNumber() - track.getLastPointWithEstimate().getPingIndex().getPingNumber();
      return missingPings > maxMissingPings;
   }
}
