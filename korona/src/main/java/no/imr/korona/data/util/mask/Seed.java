package no.imr.korona.data.util.mask;

import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.range.FloatRange;

record Seed(PingIndex pingIndex, FloatRange depthRange) {
   @Override
   public String toString() {
      return "{" + pingIndex.getPingNumber() + ", " + depthRange + "}";
   }
}
