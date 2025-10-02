package no.imr.lsss.modules.broadband.ts;

import no.imr.lsss.modules.ts.BaseTsData;
import no.imr.tools.range.FloatRange;

record BroadbandTsData(
      float depth,
      FloatRange depthRange,
      float[] values,
      FloatRange frequencyRange
) implements BaseTsData {

   float getDeltaFrequency() {
      return frequencyRange.getSize() / (values.length - 1);
   }
}
