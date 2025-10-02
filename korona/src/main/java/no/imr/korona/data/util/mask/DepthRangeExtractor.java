package no.imr.korona.data.util.mask;

import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.range.FloatRange;

import java.util.List;

@FunctionalInterface
public interface DepthRangeExtractor {
   List<FloatRange> depthRanges(PingIndex pingIndex);
}
