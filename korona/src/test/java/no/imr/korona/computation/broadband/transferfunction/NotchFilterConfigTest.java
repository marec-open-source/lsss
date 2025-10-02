package no.imr.korona.computation.broadband.transferfunction;

import no.imr.korona.computation.broadband.notchfilter.BroadbandNotchFilterConfig;
import no.imr.tools.range.FloatRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class NotchFilterConfigTest {
   @Test
   void test() {
      NotchFilterConfig a = new NotchFilterConfig(List.of(
            new BroadbandNotchFilterConfig(49_000, 5_000),
            new BroadbandNotchFilterConfig(75_000, 5_000),
            new BroadbandNotchFilterConfig(101_000, 5_000)
      ), FloatRange.of(50_000, 100_000));

      NotchFilterConfig b = new NotchFilterConfig(List.of(
            new BroadbandNotchFilterConfig(75_000, 5_000)
      ), FloatRange.of(50_000, 100_000));

      assertEquals(a, b);
      assertEquals(List.of(new BroadbandNotchFilterConfig(75_000, 5_000)), a.broadbandNotchFilterConfigs());
   }
}
