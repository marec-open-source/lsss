package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.items.channel.ChannelData;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.Variable;

import java.io.IOException;

abstract class ChannelGroupOutputBuilder {
   final long referenceTimeInMillis;
   final int channel;
   final Group.Builder groupBuilder;
   private final String fullGroupName;

   ChannelGroupOutputBuilder(Group.Builder parentGroup, int frequencyIndex, ChannelData channelData) {
      PingConfiguration pingConfiguration = channelData.getPingConfiguration();
      referenceTimeInMillis = pingConfiguration.getRawFileConfiguration().getTimeInMillis();
      channel = channelData.getChannel();

      groupBuilder = new Group.Builder()
            .setName("frequency_" + frequencyIndex)
            .addAttribute(new Attribute(Nc.CHANNEL_ID, pingConfiguration.getRawFileConfiguration().getTransducers().get(channel - 1).getChannelId()));

      parentGroup.addGroup(groupBuilder);

      fullGroupName = groupBuilder.makeFullName();
   }

   Variable findGroupVariable(NcGridWriter ncGridWriter, String variable) {
      return ncGridWriter.findVariable(fullGroupName + variable);
   }

   abstract ChannelGroupOutputWriter createWriter(NcGridWriter ncGridWriter) throws InvalidRangeException, IOException;
}
