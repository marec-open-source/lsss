package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.items.channel.ChannelData;
import org.jspecify.annotations.Nullable;
import ucar.nc2.Group;

abstract class ChannelGroupOutput {
   ChannelGroupOutput() {
   }

   abstract @Nullable ChannelGroupOutputBuilder createBuilder(Group.Builder parentGroup, int frequencyIndex, ChannelData channelData);
}
