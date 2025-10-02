package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.ChannelData;
import org.jspecify.annotations.Nullable;
import ucar.nc2.Group;

final class ChannelGroupBroadbandSvOutput extends ChannelGroupOutput {
   private final @Nullable Float optionalDeltaRange;
   private final @Nullable Float optionalMaxRange;
   private final float fftWindowSizeInPulseLengths;
   private final float deltaFrequency;
   private final boolean writeAngles;

   ChannelGroupBroadbandSvOutput(@Nullable Float optionalDeltaRange, @Nullable Float optionalMaxRange,
                                 float fftWindowSizeInPulseLengths, float deltaFrequency,
                                 boolean writeAngles) {
      this.optionalDeltaRange = optionalDeltaRange;
      this.optionalMaxRange = optionalMaxRange;
      this.fftWindowSizeInPulseLengths = fftWindowSizeInPulseLengths;
      this.deltaFrequency = deltaFrequency;
      this.writeAngles = writeAngles;
   }

   @Override
   ChannelGroupOutputBuilder createBuilder(Group.Builder parentGroup, int frequencyIndex, ChannelData channelData) {
      float deltaRange = optionalDeltaRange != null ? optionalDeltaRange : channelData.getSampleDistance();
      float maxRange = optionalMaxRange != null ? optionalMaxRange : channelData.getMaxRange();
      if (channelData instanceof BroadbandData broadbandData) {
         return new ChannelGroupBroadbandSvOutputBuilder(parentGroup, frequencyIndex, broadbandData, deltaRange, maxRange, writeAngles, fftWindowSizeInPulseLengths, deltaFrequency);
      }
      return new ChannelGroupSvAndAnglesOutputBuilder(parentGroup, frequencyIndex, channelData.getPowerData(), maxRange, writeAngles);
   }
}
