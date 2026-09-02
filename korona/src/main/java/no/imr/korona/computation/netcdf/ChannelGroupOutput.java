package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.items.channel.ChannelData;
import ucar.nc2.Group;

interface ChannelGroupOutput {
   PingByPingBuilder createBuilder(Group.Builder groupBuilder, NcConfig ncConfig, ChannelData channelData);
}
