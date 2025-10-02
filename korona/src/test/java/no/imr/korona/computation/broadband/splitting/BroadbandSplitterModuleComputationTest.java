package no.imr.korona.computation.broadband.splitting;

import no.imr.tools.range.FloatRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class BroadbandSplitterModuleComputationTest {
   @Test
   void makeAutoBands() {
      assertEquals(List.of(FloatRange.of(100, 200)),
            BroadbandSplitterModuleComputation.makeAutoBands(List.of(FloatRange.of(100, 200)), 1, 0)
                  .map(BroadbandSplitterModuleComputation.FrequencyBand::frequencyRange)
                  .toList());
      assertEquals(List.of(FloatRange.of(100, 120), FloatRange.of(120, 140), FloatRange.of(140, 160), FloatRange.of(160, 180), FloatRange.of(180, 200)),
            BroadbandSplitterModuleComputation.makeAutoBands(List.of(FloatRange.of(100, 200)), 5, 0)
                  .map(BroadbandSplitterModuleComputation.FrequencyBand::frequencyRange)
                  .toList());

      assertEquals(List.of(FloatRange.of(145, 155)),
            BroadbandSplitterModuleComputation.makeAutoBands(List.of(FloatRange.of(100, 200)), 1, 10)
                  .map(BroadbandSplitterModuleComputation.FrequencyBand::frequencyRange)
                  .toList());
      assertEquals(List.of(FloatRange.of(100, 200)),
            BroadbandSplitterModuleComputation.makeAutoBands(List.of(FloatRange.of(100, 200)), 1, 120)
                  .map(BroadbandSplitterModuleComputation.FrequencyBand::frequencyRange)
                  .toList());
      assertEquals(List.of(FloatRange.of(105, 115), FloatRange.of(125, 135), FloatRange.of(145, 155), FloatRange.of(165, 175), FloatRange.of(185, 195)),
            BroadbandSplitterModuleComputation.makeAutoBands(List.of(FloatRange.of(100, 200)), 5, 10)
                  .map(BroadbandSplitterModuleComputation.FrequencyBand::frequencyRange)
                  .toList());
      assertEquals(List.of(FloatRange.of(100, 130), FloatRange.of(110, 150), FloatRange.of(130, 170), FloatRange.of(150, 190), FloatRange.of(170, 200)),
            BroadbandSplitterModuleComputation.makeAutoBands(List.of(FloatRange.of(100, 200)), 5, 40)
                  .map(BroadbandSplitterModuleComputation.FrequencyBand::frequencyRange)
                  .toList());
   }
}
