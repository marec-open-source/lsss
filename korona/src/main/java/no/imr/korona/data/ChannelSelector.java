package no.imr.korona.data;

import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;

/**
 * For selecting channel number.
 */
@FunctionalInterface
public interface ChannelSelector {
   /**
    * This function should be able to uniquely determine a channel
    * number based on other properties of the channel, for example
    * frequency.
    *
    * @param rawFileConfiguration configuration
    * @return the "physical" channel number
    */
   int getChannel(RawFileConfiguration rawFileConfiguration);

   static ChannelSelector channelOne() {
      return __ -> 1;
   }
}
