package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.items.channel.ChannelData;

import java.util.Map;

record ChannelGroupConfig(
      ChannelGroupOutput channelGroupOutput,
      Map<Integer, ChannelData> channelToChannelData
) {
}
