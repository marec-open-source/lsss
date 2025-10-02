package no.imr.korona.data.util.mask;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class MaskOutlineTracerTest {
   @Test
   void getDepthRanges() {
      PingIndex p = PingRange.EMPTY_RANGE.begin();
      assertEquals(FloatRangeSet.of(), MaskOutlineTracer.getDepthRanges(List.of()));
      assertEquals(FloatRangeSet.of(), MaskOutlineTracer.getDepthRanges(List.of(new EchogramPoint(p, 10))));
      assertEquals(FloatRangeSet.of(List.of(
                  FloatRange.of(2, 3),
                  FloatRange.of(4, 6)
            )),
            MaskOutlineTracer.getDepthRanges(List.of(
                  new EchogramPoint(p, 1),
                  new EchogramPoint(p, 5),
                  new EchogramPoint(p, 6),
                  new EchogramPoint(p, 4),
                  new EchogramPoint(p, 5),
                  new EchogramPoint(p, 3),
                  new EchogramPoint(p, 2),
                  new EchogramPoint(p, 1)
            )));
   }
}
