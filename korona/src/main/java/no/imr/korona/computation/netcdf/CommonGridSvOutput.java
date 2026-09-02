package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.netcdf.NcBuild;
import no.imr.tools.netcdf.NcWrite;
import no.imr.tools.netcdf.NetcdfUtils;
import org.jspecify.annotations.Nullable;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.util.List;

final class CommonGridSvOutput implements CommonGridOutput {
   private final @Nullable LogSvCompressor logSvCompressor;

   CommonGridSvOutput(@Nullable LogSvCompressor logSvCompressor) {
      this.logSvCompressor = logSvCompressor;
   }

   @Override
   public PingByPingBuilder createBuilder(NcConfig ncConfig, RangeConfig rangeConfig, Group.Builder groupBuilder, Dimension frequencyDim, Dimension pingTimeDim, Dimension rangeDim) {
      List<String> coordinates = List.of(Nc.CHANNEL_ID, Nc.RAW_FILE, Nc.LATITUDE, Nc.LONGITUDE);
      if (logSvCompressor != null) {
         Variable.Builder<?> compressedLogSvBuilder = NcBuild.newVariable(Nc.LOG_SV_COMPRESSED, DataType.USHORT, List.of(frequencyDim, pingTimeDim, rangeDim));
         logSvCompressor.addAttributes(compressedLogSvBuilder);
         groupBuilder.addVariable(compressedLogSvBuilder);
      } else {
         groupBuilder.addVariable(NcBuild.floatVariable(Nc.SV, List.of(frequencyDim, pingTimeDim, rangeDim), coordinates));
      }

      return (writer, group) -> {
         return logSvCompressor == null
               ? svWriter(writer, group, ncConfig, rangeConfig)
               : compressedSvWriter(writer, group, ncConfig, rangeConfig, logSvCompressor);
      };
   }

   private static PingByPingWriter svWriter(NetcdfFormatWriter writer, Group group, NcConfig ncConfig, RangeConfig rangeConfig) throws IOException {
      Variable svVar = NetcdfUtils.findVariable(group, Nc.SV);

      return (ping, pingTimeIndex) -> {
         for (int channelIndex = 0; channelIndex < ncConfig.channelCount; channelIndex++) {
            PowerData powerData = ping.getPowerData(channelIndex + 1);
            if (powerData == null) {
               continue;
            }
            int frequencyIndex = ncConfig.channelIndexToNcFrequencyIndex(channelIndex);

            OffsetValues resampledSv = rangeConfig.resample(powerData, powerData.getSv());
            if (resampledSv != null) {
               float[] resampledSvValues = resampledSv.values().clone(); // Copy to not change values in the ping.
               ArrayMath.divide(resampledSvValues, PowerData.IMR_CONSTANT);
               NcWrite.floatD3(writer, svVar, frequencyIndex, pingTimeIndex, resampledSv.offset(), resampledSvValues);
            }
         }
      };
   }

   private static PingByPingWriter compressedSvWriter(NetcdfFormatWriter writer, Group group, NcConfig ncConfig, RangeConfig rangeConfig, LogSvCompressor logSvCompressor) throws IOException {
      Variable compressedLogSvVar = NetcdfUtils.findVariable(group, Nc.LOG_SV_COMPRESSED);

      return (ping, pingTimeIndex) -> {
         for (int channelIndex = 0; channelIndex < ncConfig.channelCount; channelIndex++) {
            PowerData powerData = ping.getPowerData(channelIndex + 1);
            if (powerData == null) {
               continue;
            }
            int frequencyIndex = ncConfig.channelIndexToNcFrequencyIndex(channelIndex);

            OffsetValues resampledSv = rangeConfig.resample(powerData, powerData.getSv());
            if (resampledSv != null) {
               short[] logSvCompressed = logSvCompressor.compressSv(resampledSv.values());
               NcWrite.shortD3(writer, compressedLogSvVar, frequencyIndex, pingTimeIndex, resampledSv.offset(), logSvCompressed);
            }
         }
      };
   }
}
