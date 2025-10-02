package no.imr.lsss.modules.korona.tracking;

import no.imr.lsss.modules.ts.BaseTsData;
import no.imr.tools.range.FloatRange;

public record TrackBorder(
      TrackId trackId,
      int channel,
      FloatRange depthRange,
      float peakDepth,
      boolean useAngles
) implements BaseTsData {
   @Override
   public String toString() {
      return trackId + ", " + depthRange;
   }

   @Override
   public float depth() {
      return peakDepth;
   }

   TrackBorder withId(TrackId trackId) {
      return new TrackBorder(trackId, channel, depthRange, peakDepth, useAngles);
   }
}
