package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.Ping;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;

abstract class ChannelGroupOutputWriter {
   final NcGridWriter ncGridWriter;
   final NetcdfFormatWriter writer;
   final int channel;

   ChannelGroupOutputWriter(NcGridWriter ncGridWriter, ChannelGroupOutputBuilder channelGroupOutputBuilder) {
      this.ncGridWriter = ncGridWriter;
      writer = ncGridWriter.getWriter();
      channel = channelGroupOutputBuilder.channel;
   }

   abstract void write(Ping ping, int pingTimeIndex) throws InvalidRangeException, IOException;
}
