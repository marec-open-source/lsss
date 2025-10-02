package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.ChannelData;
import org.jspecify.annotations.Nullable;
import ucar.nc2.Group;

final class ChannelGroupPulseCompressionOutput extends ChannelGroupOutput {
   private final @Nullable Float optionalMaxRange;
   private final boolean writeAngles;

   ChannelGroupPulseCompressionOutput(@Nullable Float optionalMaxRange, boolean writeAngles) {
      this.optionalMaxRange = optionalMaxRange;
      this.writeAngles = writeAngles;
   }

   @Override
   ChannelGroupOutputBuilder createBuilder(Group.Builder parentGroup, int frequencyIndex, ChannelData channelData) {
      float maxRange = optionalMaxRange != null ? optionalMaxRange : channelData.getMaxRange();
      if (channelData instanceof BroadbandData broadbandData) {
         return new ChannelGroupPulseCompressionOutputBuilder(parentGroup, frequencyIndex, broadbandData, maxRange, writeAngles);
      }
      return new ChannelGroupSvAndAnglesOutputBuilder(parentGroup, frequencyIndex, channelData.getPowerData(), maxRange, writeAngles);
   }
}
