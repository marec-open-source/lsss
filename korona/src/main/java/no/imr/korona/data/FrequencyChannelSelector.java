package no.imr.korona.data;

import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.Utils;

/**
 * Class for getting channel number for a raw data channel based on frequency.
 */
public final class FrequencyChannelSelector implements ChannelSelector {
   private final float frequency;

   public FrequencyChannelSelector(float frequency) {
      this.frequency = frequency;
   }

   @Override
   public int getChannel(RawFileConfiguration rawFileConfiguration) {
      return rawFileConfiguration.lastChannelWithKHz(getKHz());
   }

   public float getHz() {
      return frequency;
   }

   public int getKHz() {
      return Utils.hzToKHz(frequency);
   }
}
