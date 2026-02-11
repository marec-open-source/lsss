package no.imr.korona.data.util.mask;

import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.range.FloatRangeSet;

@FunctionalInterface
public interface DepthRangeExtractor {
   FloatRangeSet depthRanges(PingIndex pingIndex);
}
