package no.imr.korona.computation.netcdf;

import no.imr.korona.Korona;
import no.imr.korona.data.ping.Ping;
import no.imr.tools.Utils;
import no.imr.tools.netcdf.NcBuild;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

final class NcGridWriter implements AutoCloseable {
   private final NetcdfFormatWriter writer;
   private int pingTimeIndex;
   private final PingByPingWriter pingByPingWriter;

   NcGridWriter(Path file, List<PingByPingOutput> pingByPingOutputs, NcConfig ncConfig) throws IOException, InvalidRangeException {

      NetcdfFormatWriter.Builder fileBuilder = NcBuild.newBuilder(file);
      Group.Builder groupBuilder = fileBuilder.getRootGroup()
            .addAttribute(new Attribute("name", "KORONA"))
            .addAttribute(new Attribute("description", "Multi-frequency echosounder data"))
            .addAttribute(new Attribute("time", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()))
            .addAttribute(new Attribute("version", Korona.VERSION))
            .addAttribute(new Attribute("git_commit", Utils.GIT_COMMIT));

      List<PingByPingBuilder> pingByPingBuilders = new ArrayList<>();
      for (PingByPingOutput pingByPingOutput : pingByPingOutputs) {
         pingByPingBuilders.add(pingByPingOutput.createBuilder(groupBuilder, ncConfig));
      }

      writer = fileBuilder.build();

      try {
         Group group = writer.getOutputFile().getRootGroup();
         List<PingByPingWriter> pingByPingWriters = new ArrayList<>();
         for (PingByPingBuilder pingByPingBuilder : pingByPingBuilders) {
            pingByPingWriters.add(pingByPingBuilder.createWriter(writer, group));
         }
         pingByPingWriter = PingByPingWriter.of(pingByPingWriters);
      } catch (Exception e) {
         Utils.closeOrSuppress(e, writer);
         throw e;
      }
   }

   void writePing(Ping ping) throws InvalidRangeException, IOException {
      pingByPingWriter.write(ping, pingTimeIndex);
      pingTimeIndex++;
   }

   @Override
   public void close() throws IOException {
      writer.close();
   }
}
