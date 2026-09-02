package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.netcdf.NcBuild;
import no.imr.tools.netcdf.NcWrite;
import no.imr.tools.netcdf.NetcdfUtils;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Group;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.util.List;

final class EnvironmentGroupOutput implements PingByPingOutput {
   EnvironmentGroupOutput() {
   }

   @Override
   public PingByPingBuilder createBuilder(Group.Builder groupBuilder, NcConfig ncConfig) {
      Group.Builder environmentGroupBuilder = new Group.Builder()
            .setName(Nc.ENVIRONMENT_GROUP);
      groupBuilder.addGroup(environmentGroupBuilder);

      environmentGroupBuilder.addVariable(NcBuild.floatVariable(Nc.ENVIRONMENT_DEPTH, List.of()));
      environmentGroupBuilder.addVariable(NcBuild.floatVariable(Nc.ENVIRONMENT_ACIDITY, List.of()));
      environmentGroupBuilder.addVariable(NcBuild.floatVariable(Nc.ENVIRONMENT_SALINITY, List.of()));
      environmentGroupBuilder.addVariable(NcBuild.floatVariable(Nc.ENVIRONMENT_TEMPERATURE, List.of()));

      return (writer, group) -> {
         writeUpFront(writer, group, ncConfig);
         return PingByPingWriter.empty();
      };
   }

   private static void writeUpFront(NetcdfFormatWriter writer, Group group, NcConfig ncConfig) throws InvalidRangeException, IOException {
      RawFileConfiguration.Xml0Info xml0Info = ncConfig.pingConfiguration.getRawFileConfiguration().getXml0Info();
      if (xml0Info != null) {
         Group environmentGroup = NetcdfUtils.findGroup(group, Nc.ENVIRONMENT_GROUP);
         NcWrite.floatD0(writer, NetcdfUtils.findVariable(environmentGroup, Nc.ENVIRONMENT_DEPTH), xml0Info.getDepth());
         NcWrite.floatD0(writer, NetcdfUtils.findVariable(environmentGroup, Nc.ENVIRONMENT_ACIDITY), xml0Info.getAcidity());
         NcWrite.floatD0(writer, NetcdfUtils.findVariable(environmentGroup, Nc.ENVIRONMENT_SALINITY), xml0Info.getSalinity());
         NcWrite.floatD0(writer, NetcdfUtils.findVariable(environmentGroup, Nc.ENVIRONMENT_TEMPERATURE), xml0Info.getTemperature());
      }
   }
}
