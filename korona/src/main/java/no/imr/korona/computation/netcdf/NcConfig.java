package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.PingConfiguration;
import no.imr.tools.math.ArrayMath;

import java.time.Instant;

final class NcConfig {
   final PingConfiguration pingConfiguration;
   final int channelCount;
   final Instant referenceTime;

   private final int[] channelIndexToNcFrequencyIndex;

   NcConfig(PingConfiguration pingConfiguration, int referenceChannel) {
      this.pingConfiguration = pingConfiguration;
      channelCount = pingConfiguration.getRawFileConfiguration().getTransducerCount();
      referenceTime = pingConfiguration.getRawFileConfiguration().getInstant();

      channelIndexToNcFrequencyIndex = new int[channelCount];
      for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
         channelIndexToNcFrequencyIndex[channelIndex] = channelIndex;
      }
      ArrayMath.swap(channelIndexToNcFrequencyIndex, 0, referenceChannel - 1); // Place reference frequency first.
   }

   int channelIndexToNcFrequencyIndex(int channelIndex) {
      return channelIndexToNcFrequencyIndex[channelIndex];
   }
}
