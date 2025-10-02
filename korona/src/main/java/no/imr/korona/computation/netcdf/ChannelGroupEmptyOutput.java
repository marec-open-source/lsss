package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.items.channel.ChannelData;
import org.jspecify.annotations.Nullable;
import ucar.nc2.Group;

final class ChannelGroupEmptyOutput extends ChannelGroupOutput {
   ChannelGroupEmptyOutput() {
   }

   @Override
   @Nullable ChannelGroupOutputBuilder createBuilder(Group.Builder parentGroup, int frequencyIndex, ChannelData channelData) {
      return null;
   }
}
