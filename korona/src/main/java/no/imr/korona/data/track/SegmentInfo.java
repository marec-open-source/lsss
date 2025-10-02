package no.imr.korona.data.track;

import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;

/**
 * Quick to load info about a {@link SegmentHandle}.
 */
public final class SegmentInfo {
   private final RawFileConfigurationInfo rawFileConfigurationInfo;
   private final PingRange pingRange;

   public SegmentInfo(RawFileConfigurationInfo rawFileConfigurationInfo, PingRange pingRange) {
      this.rawFileConfigurationInfo = rawFileConfigurationInfo;
      this.pingRange = pingRange;
   }

   public SegmentInfo(RawFileConfiguration rawFileConfiguration, PingRange pingRange) {
      this(RawFileConfigurationInfo.of(rawFileConfiguration), pingRange);
   }

   public RawFileConfigurationInfo getRawFileConfigurationInfo() {
      return rawFileConfigurationInfo;
   }

   public PingRange getPingRange() {
      return pingRange;
   }

   @Override
   public String toString() {
      return pingRange + ", " + rawFileConfigurationInfo;
   }
}
