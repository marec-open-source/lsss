package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.netcdf.NcWrite;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.util.List;

final class GridPulseCompressionOutput extends GridOutput {
   GridPulseCompressionOutput() {
   }

   @Override
   void addVariables(Group.Builder builder, Dimension frequencyDim, Dimension pingTimeDim, Dimension rangeDim) {
      List<String> coordinates = List.of(Nc.CHANNEL_ID, Nc.RAW_FILE, Nc.LATITUDE, Nc.LONGITUDE);
      NcWrite.addFloatVariable(builder, Nc.PULSE_COMPRESSED_RE, List.of(frequencyDim, pingTimeDim, rangeDim), coordinates);
      NcWrite.addFloatVariable(builder, Nc.PULSE_COMPRESSED_IM, List.of(frequencyDim, pingTimeDim, rangeDim), coordinates);
   }

   @Override
   GridOutputWriter createWriter(NcGridWriter ncGridWriter) {
      return new GridPulseCompressionWriter(ncGridWriter);
   }

   private static final class GridPulseCompressionWriter extends GridOutputWriter {
      private final NcGridWriter ncGridWriter;
      private final NetcdfFormatWriter writer;
      private final int[] channelIndexToNetcdfFrequencyIndex;
      private final int channelCount;

      private final Variable pulseCompressedReVar;
      private final Variable pulseCompressedImVar;

      private GridPulseCompressionWriter(NcGridWriter ncGridWriter) {
         this.ncGridWriter = ncGridWriter;
         writer = ncGridWriter.getWriter();
         channelIndexToNetcdfFrequencyIndex = ncGridWriter.getChannelIndexToNetcdfFrequencyIndex();
         channelCount = channelIndexToNetcdfFrequencyIndex.length;

         pulseCompressedReVar = ncGridWriter.findVariable(Nc.PULSE_COMPRESSED_RE);
         pulseCompressedImVar = ncGridWriter.findVariable(Nc.PULSE_COMPRESSED_IM);
      }

      @Override
      void write(Ping ping, int pingTimeIndex) throws InvalidRangeException, IOException {
         for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
            BroadbandData broadbandData = ping.getBroadbandData(channelIndex + 1);
            if (broadbandData == null) {
               continue;
            }
            int frequencyIndex = channelIndexToNetcdfFrequencyIndex[channelIndex];

            ComplexArray averagePulseCompressedSignal = broadbandData.getAveragePulseCompressedSignal();
            float[] re = new float[averagePulseCompressedSignal.length()];
            float[] im = new float[averagePulseCompressedSignal.length()];
            for (int i = 0; i < re.length; i++) {
               re[i] = (float) averagePulseCompressedSignal.re(i);
               im[i] = (float) averagePulseCompressedSignal.im(i);
            }
            OffsetValues resampledRe = ncGridWriter.resample(broadbandData, re);
            if (resampledRe != null) {
               writer.write(pulseCompressedReVar, new int[]{frequencyIndex, pingTimeIndex, resampledRe.offset},
                     Array.makeFromJavaArray(new float[][][]{{resampledRe.values}}));
            }
            OffsetValues resampledIm = ncGridWriter.resample(broadbandData, im);
            if (resampledIm != null) {
               writer.write(pulseCompressedImVar, new int[]{frequencyIndex, pingTimeIndex, resampledIm.offset},
                     Array.makeFromJavaArray(new float[][][]{{resampledIm.values}}));
            }
         }
      }
   }
}
