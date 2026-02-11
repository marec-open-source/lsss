package no.imr.korona.data.track;

import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;

/**
 * Quick to load info about a {@link SegmentHandle}.
 */
public record SegmentInfo(
      RawFileConfigurationInfo rawFileConfigurationInfo,
      PingRange pingRange
) {
   public SegmentInfo(RawFileConfiguration rawFileConfiguration, PingRange pingRange) {
      this(RawFileConfigurationInfo.of(rawFileConfiguration), pingRange);
   }

   @Override
   public String toString() {
      return pingRange + ", " + rawFileConfigurationInfo;
   }
}
