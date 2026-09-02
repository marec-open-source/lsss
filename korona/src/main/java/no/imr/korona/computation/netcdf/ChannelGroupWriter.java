package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.tools.netcdf.NetcdfUtils;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ChannelGroupWriter implements PingByPingOutput {
   private final List<ChannelGroupOutput> channelGroupOutputs;
   private final Map<Integer, ChannelData> channelToChannelData;

   ChannelGroupWriter(List<ChannelGroupOutput> channelGroupOutputs, Map<Integer, ChannelData> channelToChannelData) {
      this.channelGroupOutputs = channelGroupOutputs;
      this.channelToChannelData = channelToChannelData;
   }

   @Override
   public PingByPingBuilder createBuilder(Group.Builder groupBuilder, NcConfig ncConfig) {
      List<NamedAndBuilder> namedAndBuilders = new ArrayList<>();
      for (int channelIndex = 0; channelIndex < ncConfig.channelCount; channelIndex++) {
         ChannelData channelData = channelToChannelData.get(channelIndex + 1);
         if (channelData != null) {
            int frequencyIndex = ncConfig.channelIndexToNcFrequencyIndex(channelIndex);
            String channelGroupName = "frequency_" + frequencyIndex;
            Group.Builder channelGroupBuilder = new Group.Builder()
                  .setName(channelGroupName)
                  .addAttribute(new Attribute(Nc.CHANNEL_ID, ncConfig.pingConfiguration.getRawFileConfiguration().getTransducers().get(channelIndex).getChannelId()));
            groupBuilder.addGroup(channelGroupBuilder);
            List<PingByPingBuilder> pingByPingBuilders = new ArrayList<>();
            for (ChannelGroupOutput channelGroupOutput : channelGroupOutputs) {
               pingByPingBuilders.add(channelGroupOutput.createBuilder(channelGroupBuilder, ncConfig, channelData));
            }
            namedAndBuilders.add(new NamedAndBuilder(channelGroupName, pingByPingBuilders));
         }
      }

      return (writer, group) -> {
         return createPingByPingWriter(writer, group, namedAndBuilders);
      };
   }

   private static PingByPingWriter createPingByPingWriter(NetcdfFormatWriter writer, Group group, List<NamedAndBuilder> namedAndBuilders) throws InvalidRangeException, IOException {
      List<PingByPingWriter> pingByPingWriters = new ArrayList<>();
      for (NamedAndBuilder namedAndBuilder : namedAndBuilders) {
         Group channelGroup = NetcdfUtils.findGroup(group, namedAndBuilder.groupName);
         for (PingByPingBuilder pingByPingBuilder : namedAndBuilder.pingByPingBuilders) {
            pingByPingWriters.add(pingByPingBuilder.createWriter(writer, channelGroup));
         }
      }
      return PingByPingWriter.of(pingByPingWriters);
   }

   private record NamedAndBuilder(String groupName, List<PingByPingBuilder> pingByPingBuilders) {
   }
}
