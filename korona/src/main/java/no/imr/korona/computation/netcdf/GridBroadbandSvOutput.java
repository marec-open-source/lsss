package no.imr.korona.computation.netcdf;

import no.imr.korona.computation.broadband.BroadbandSvByFrequency;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.netcdf.NcWrite;
import no.imr.tools.range.FloatRange;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.util.List;

final class GridBroadbandSvOutput extends GridOutput {
   private final float fftWindowSizeInPulseLengths;
   private final FloatRange totalFrequencyRange;
   private final float deltaFrequency;
   private final int broadbandFrequencyLength;

   GridBroadbandSvOutput(float fftWindowSizeInPulseLengths, FloatRange totalFrequencyRange, float deltaFrequency) {
      this.fftWindowSizeInPulseLengths = fftWindowSizeInPulseLengths;
      this.totalFrequencyRange = totalFrequencyRange.roundToMultipleOf(deltaFrequency);
      this.deltaFrequency = deltaFrequency;
      broadbandFrequencyLength = Math.round(totalFrequencyRange.getSize() / deltaFrequency) + 1;
   }

   @Override
   void addVariables(Group.Builder builder, Dimension frequencyDim, Dimension pingTimeDim, Dimension rangeDim) {
      Dimension broadbandFrequencyDim = NcWrite.addDimension(builder, Nc.BROADBAND_FREQUENCY, broadbandFrequencyLength);

      NcWrite.addFloatVariable(builder, Nc.BROADBAND_FREQUENCY, List.of(broadbandFrequencyDim), List.of());
      NcWrite.addFloatVariable(builder, Nc.BROADBAND_SV, List.of(pingTimeDim, rangeDim, broadbandFrequencyDim), List.of());
   }

   @Override
   GridOutputWriter createWriter(NcGridWriter ncGridWriter) throws InvalidRangeException, IOException {
      return new GridBroadbandSvWriter(ncGridWriter, this);
   }

   private static final class GridBroadbandSvWriter extends GridOutputWriter {
      private final NcGridWriter ncGridWriter;
      private final GridBroadbandSvOutput broadbandSvOutput;
      private final NetcdfFormatWriter writer;

      private final Variable broadbandSvVar;

      private GridBroadbandSvWriter(NcGridWriter ncGridWriter, GridBroadbandSvOutput broadbandSvOutput) throws InvalidRangeException, IOException {
         this.ncGridWriter = ncGridWriter;
         this.broadbandSvOutput = broadbandSvOutput;
         writer = ncGridWriter.getWriter();

         Variable broadbandFrequencyVar = ncGridWriter.findVariable(Nc.BROADBAND_FREQUENCY);
         broadbandSvVar = ncGridWriter.findVariable(Nc.BROADBAND_SV);

         float[] broadbandFrequencies = new float[broadbandSvOutput.broadbandFrequencyLength];
         for (int i = 0; i < broadbandFrequencies.length; i++) {
            broadbandFrequencies[i] = broadbandSvOutput.totalFrequencyRange.min() + i * broadbandSvOutput.deltaFrequency;
         }
         writer.write(broadbandFrequencyVar, new int[]{0}, Array.makeFromJavaArray(broadbandFrequencies));
      }

      @Override
      void write(Ping ping, int pingTimeIndex) throws InvalidRangeException, IOException {
         int rangeLength = ncGridWriter.getRangeLength();
         float deltaRange = ncGridWriter.getDeltaRange();
         int channelCount = ncGridWriter.getChannelIndexToNetcdfFrequencyIndex().length;

         for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
            BroadbandData broadbandData = ping.getBroadbandData(channelIndex + 1);
            if (broadbandData == null) {
               continue;
            }
            BroadbandSvByFrequency broadbandSvByFrequency = new BroadbandSvByFrequency(broadbandData);

            FloatRange frequencyRange = broadbandData.getFrequencyRange();
            float pulseLength = broadbandData.getSoundVelocity() * broadbandData.getPulseDuration();
            float fftWindowRadius = broadbandSvOutput.fftWindowSizeInPulseLengths * pulseLength / 2;

            for (int iRange = 0; iRange < rangeLength; iRange++) {
               float centerRange = (iRange + 0.5f) * deltaRange;
               float minRange = centerRange - fftWindowRadius;
               int iBegin = broadbandData.rangeToSampleIndex(minRange);
               if (iBegin < 0) {
                  continue;
               }
               float maxRange = centerRange + fftWindowRadius;
               int iEnd = broadbandData.rangeToSampleIndex(maxRange);
               if (iEnd > broadbandData.getCount()) {
                  break;
               }
               float[] sv = broadbandSvByFrequency.calculate(iBegin, iEnd, frequencyRange);
               OffsetValues resampledSv = OffsetValues.resample(sv, frequencyRange, broadbandSvOutput.totalFrequencyRange, broadbandSvOutput.broadbandFrequencyLength);
               if (resampledSv != null) {
                  ArrayMath.divide(resampledSv.values, PowerData.IMR_CONSTANT);
                  writer.write(broadbandSvVar, new int[]{pingTimeIndex, iRange, resampledSv.offset},
                        Array.makeFromJavaArray(new float[][][]{{resampledSv.values}}));
               }
            }
         }
      }
   }
}
