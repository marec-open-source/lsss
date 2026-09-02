package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.netcdf.NcBuild;
import no.imr.tools.netcdf.NcWrite;
import no.imr.tools.netcdf.NetcdfUtils;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.util.List;

final class CommonGridPulseCompressionOutput implements CommonGridOutput {
   CommonGridPulseCompressionOutput() {
   }

   @Override
   public PingByPingBuilder createBuilder(NcConfig ncConfig, RangeConfig rangeConfig, Group.Builder groupBuilder, Dimension frequencyDim, Dimension pingTimeDim, Dimension rangeDim) {
      List<String> coordinates = List.of(Nc.CHANNEL_ID, Nc.RAW_FILE, Nc.LATITUDE, Nc.LONGITUDE);
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.PULSE_COMPRESSED_RE, List.of(frequencyDim, pingTimeDim, rangeDim), coordinates));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.PULSE_COMPRESSED_IM, List.of(frequencyDim, pingTimeDim, rangeDim), coordinates));

      return (writer, group) -> {
         return createPingByPingWriter(writer, group, ncConfig, rangeConfig);
      };
   }

   private static PingByPingWriter createPingByPingWriter(NetcdfFormatWriter writer, Group group, NcConfig ncConfig, RangeConfig rangeConfig) throws IOException {
      Variable pulseCompressedReVar = NetcdfUtils.findVariable(group, Nc.PULSE_COMPRESSED_RE);
      Variable pulseCompressedImVar = NetcdfUtils.findVariable(group, Nc.PULSE_COMPRESSED_IM);

      return (ping, pingTimeIndex) -> {
         for (int channelIndex = 0; channelIndex < ncConfig.channelCount; channelIndex++) {
            BroadbandData broadbandData = ping.getBroadbandData(channelIndex + 1);
            if (broadbandData == null) {
               continue;
            }
            int frequencyIndex = ncConfig.channelIndexToNcFrequencyIndex(channelIndex);

            ComplexArray averagePulseCompressedSignal = broadbandData.getAveragePulseCompressedSignal();
            float[] re = new float[averagePulseCompressedSignal.length()];
            float[] im = new float[averagePulseCompressedSignal.length()];
            for (int i = 0; i < re.length; i++) {
               re[i] = (float) averagePulseCompressedSignal.re(i);
               im[i] = (float) averagePulseCompressedSignal.im(i);
            }
            OffsetValues resampledRe = rangeConfig.resample(broadbandData, re);
            if (resampledRe != null) {
               NcWrite.floatD3(writer, pulseCompressedReVar, frequencyIndex, pingTimeIndex, resampledRe.offset(), resampledRe.values());
            }
            OffsetValues resampledIm = rangeConfig.resample(broadbandData, im);
            if (resampledIm != null) {
               NcWrite.floatD3(writer, pulseCompressedImVar, frequencyIndex, pingTimeIndex, resampledIm.offset(), resampledIm.values());
            }
         }
      };
   }
}
