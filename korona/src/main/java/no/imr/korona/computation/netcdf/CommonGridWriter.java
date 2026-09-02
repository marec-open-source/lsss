package no.imr.korona.computation.netcdf;

import no.imr.tools.netcdf.NcBuild;
import no.imr.tools.netcdf.NetcdfUtils;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class CommonGridWriter implements PingByPingOutput {
   private final List<CommonGridOutput> commonGridOutputs;
   private final RangeConfig rangeConfig;

   CommonGridWriter(List<CommonGridOutput> commonGridOutputs, float deltaRange, float maxRange) {
      this.commonGridOutputs = commonGridOutputs;
      int rangeLength = (int) Math.floor(maxRange / deltaRange);
      rangeConfig = new RangeConfig(0, deltaRange, rangeLength);
   }

   @Override
   public PingByPingBuilder createBuilder(Group.Builder groupBuilder, NcConfig ncConfig) {
      Dimension rangeDim = NcBuild.addDimension(groupBuilder, Nc.RANGE, rangeConfig.length());
      groupBuilder.addVariable(NcBuild.doubleVariable(Nc.RANGE, List.of(rangeDim)));

      Dimension frequencyDim = NcBuild.findDimensionLocal(groupBuilder, Nc.FREQUENCY);
      Dimension pingTimeDim = NcBuild.findDimensionLocal(groupBuilder, Nc.PING_TIME);

      List<PingByPingBuilder> pingByPingBuilders = new ArrayList<>();
      for (CommonGridOutput commonGridOutput : commonGridOutputs) {
         pingByPingBuilders.add(commonGridOutput.createBuilder(ncConfig, rangeConfig, groupBuilder, frequencyDim, pingTimeDim, rangeDim));
      }

      return (writer, group) -> {
         writeUpFront(writer, group);
         return createPingByPingWriter(writer, group, pingByPingBuilders);
      };
   }

   private void writeUpFront(NetcdfFormatWriter writer, Group group) throws IOException, InvalidRangeException {
      writer.write(NetcdfUtils.findVariable(group, Nc.RANGE), rangeConfig.toArray());
   }

   private static PingByPingWriter createPingByPingWriter(NetcdfFormatWriter writer, Group group, List<PingByPingBuilder> pingByPingBuilders) throws InvalidRangeException, IOException {
      List<PingByPingWriter> pingByPingWriters = new ArrayList<>();
      for (PingByPingBuilder pingByPingBuilder : pingByPingBuilders) {
         pingByPingWriters.add(pingByPingBuilder.createWriter(writer, group));
      }
      return PingByPingWriter.of(pingByPingWriters);
   }
}
