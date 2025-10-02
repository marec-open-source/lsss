package no.imr.korona.computation.broadband.transferfunction;

import no.imr.korona.computation.broadband.notchfilter.BroadbandNotchFilterConfig;
import no.imr.tools.range.FloatRange;

import java.util.List;

public record NotchFilterConfig(List<BroadbandNotchFilterConfig> broadbandNotchFilterConfigs, FloatRange frequencyRange) {
   public NotchFilterConfig {
      broadbandNotchFilterConfigs = broadbandNotchFilterConfigs.stream()
            .filter(config -> frequencyRange.contains(config.rejectionFrequency()))
            .toList();
   }
}
