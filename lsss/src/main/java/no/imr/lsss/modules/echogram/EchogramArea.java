package no.imr.lsss.modules.echogram;

import no.imr.korona.data.ping.PingRange;
import no.imr.tools.range.FloatRange;

public record EchogramArea(
      PingRange pingRange,
      FloatRange zRange,
      int width,
      int height
) {
   @Override
   public boolean equals(Object obj) {
      // Need identity semantics here since a new instance is created for other changes,
      // such as PingMapping and DataLoadingMode.
      return this == obj;
   }

   @Override
   public int hashCode() {
      return System.identityHashCode(this);
   }
}
