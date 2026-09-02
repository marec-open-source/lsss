package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.netcdf.NcBuild;
import no.imr.tools.netcdf.NcWrite;
import no.imr.tools.netcdf.NetcdfUtils;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.util.List;

final class CommonGridAnglesOutput implements CommonGridOutput {
   CommonGridAnglesOutput() {
   }

   @Override
   public PingByPingBuilder createBuilder(NcConfig ncConfig, RangeConfig rangeConfig, Group.Builder groupBuilder, Dimension frequencyDim, Dimension pingTimeDim, Dimension rangeDim) {
      List<String> coordinates = List.of(Nc.CHANNEL_ID, Nc.RAW_FILE, Nc.LATITUDE, Nc.LONGITUDE);
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.ANGLE_ALONGSHIP, List.of(frequencyDim, pingTimeDim, rangeDim), coordinates));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.ANGLE_ATHWARTSHIP, List.of(frequencyDim, pingTimeDim, rangeDim), coordinates));

      return (writer, group) -> {
         return anglesWriter(writer, group, ncConfig, rangeConfig);
      };
   }

   private static PingByPingWriter anglesWriter(NetcdfFormatWriter writer, Group group, NcConfig ncConfig, RangeConfig rangeConfig) throws IOException {
      Variable angleAlongshipVar = NetcdfUtils.findVariable(group, Nc.ANGLE_ALONGSHIP);
      Variable angleAthwartshipVar = NetcdfUtils.findVariable(group, Nc.ANGLE_ATHWARTSHIP);

      return (ping, pingTimeIndex) -> {
         for (int channelIndex = 0; channelIndex < ncConfig.channelCount; channelIndex++) {
            PowerData powerData = ping.getPowerData(channelIndex + 1);
            if (powerData == null) {
               continue;
            }
            AngleData angleData = powerData.getAngleData();
            if (angleData == null) {
               continue;
            }

            int frequencyIndex = ncConfig.channelIndexToNcFrequencyIndex(channelIndex);

            int count = powerData.getCount();
            float[] alongAngles = new float[count];
            float[] athwartAngles = new float[count];
            RawFileTransducer transducer = powerData.getTransducer();
            for (int i = 0; i < count; i++) {
               alongAngles[i] = angleData.getMechanicalAlongAngle(i, transducer);
               athwartAngles[i] = angleData.getMechanicalAthwartAngle(i, transducer);
            }
            OffsetValues resampledAlongAngles = rangeConfig.resample(powerData, alongAngles);
            if (resampledAlongAngles != null) {
               NcWrite.floatD3(writer, angleAlongshipVar, frequencyIndex, pingTimeIndex, resampledAlongAngles.offset(), resampledAlongAngles.values());
            }
            OffsetValues resampledAthwartAngles = rangeConfig.resample(powerData, athwartAngles);
            if (resampledAthwartAngles != null) {
               NcWrite.floatD3(writer, angleAthwartshipVar, frequencyIndex, pingTimeIndex, resampledAthwartAngles.offset(), resampledAthwartAngles.values());
            }
         }
      };
   }
}
