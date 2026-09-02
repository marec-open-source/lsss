package no.imr.korona.computation.netcdf;

import no.imr.korona.computation.broadband.BroadbandSvByFrequency;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.netcdf.NcBuild;
import no.imr.tools.netcdf.NcWrite;
import no.imr.tools.netcdf.NetcdfUtils;
import no.imr.tools.range.FloatRange;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.util.List;

final class CommonGridBroadbandSvOutput implements CommonGridOutput {
   private final float fftWindowSizeInPulseLengths;
   private final FloatRange totalFrequencyRange;
   private final float deltaFrequency;
   private final int broadbandFrequencyLength;

   CommonGridBroadbandSvOutput(float fftWindowSizeInPulseLengths, FloatRange totalFrequencyRange, float deltaFrequency) {
      this.fftWindowSizeInPulseLengths = fftWindowSizeInPulseLengths;
      this.totalFrequencyRange = totalFrequencyRange.roundToMultipleOf(deltaFrequency);
      this.deltaFrequency = deltaFrequency;
      broadbandFrequencyLength = Math.round(totalFrequencyRange.getSize() / deltaFrequency) + 1;
   }

   @Override
   public PingByPingBuilder createBuilder(NcConfig ncConfig, RangeConfig rangeConfig, Group.Builder groupBuilder, Dimension frequencyDim, Dimension pingTimeDim, Dimension rangeDim) {
      Dimension broadbandFrequencyDim = NcBuild.addDimension(groupBuilder, Nc.BROADBAND_FREQUENCY, broadbandFrequencyLength);
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.BROADBAND_FREQUENCY, List.of(broadbandFrequencyDim)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.BROADBAND_SV, List.of(pingTimeDim, rangeDim, broadbandFrequencyDim)));

      return (writer, group) -> {
         writeUpFront(writer, group);
         return createPingByPingWriter(writer, group, ncConfig, rangeConfig);
      };
   }

   private void writeUpFront(NetcdfFormatWriter writer, Group group) throws IOException, InvalidRangeException {
      float[] broadbandFrequencies = new float[broadbandFrequencyLength];
      for (int i = 0; i < broadbandFrequencies.length; i++) {
         broadbandFrequencies[i] = totalFrequencyRange.min() + i * deltaFrequency;
      }
      writer.write(NetcdfUtils.findVariable(group, Nc.BROADBAND_FREQUENCY), Array.makeFromJavaArray(broadbandFrequencies));
   }

   private PingByPingWriter createPingByPingWriter(NetcdfFormatWriter writer, Group group, NcConfig ncConfig, RangeConfig rangeConfig) throws IOException {
      Variable broadbandSvVar = NetcdfUtils.findVariable(group, Nc.BROADBAND_SV);

      return (ping, pingTimeIndex) -> {
         for (int channelIndex = 0; channelIndex < ncConfig.channelCount; channelIndex++) {
            BroadbandData broadbandData = ping.getBroadbandData(channelIndex + 1);
            if (broadbandData == null) {
               continue;
            }
            BroadbandSvByFrequency broadbandSvByFrequency = new BroadbandSvByFrequency(broadbandData);

            FloatRange frequencyRange = broadbandData.getFrequencyRange();
            float pulseLength = broadbandData.getSoundVelocity() * broadbandData.getPulseDuration();
            float fftWindowRadius = fftWindowSizeInPulseLengths * pulseLength / 2;

            for (int iRange = 0; iRange < rangeConfig.length(); iRange++) {
               float centerRange = rangeConfig.centerRange(iRange);
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
               OffsetValues resampledSv = OffsetValues.resample(sv, frequencyRange, totalFrequencyRange, broadbandFrequencyLength);
               if (resampledSv != null) {
                  ArrayMath.divide(resampledSv.values(), PowerData.IMR_CONSTANT);
                  NcWrite.floatD3(writer, broadbandSvVar, pingTimeIndex, iRange, resampledSv.offset(), resampledSv.values());
               }
            }
         }
      };
   }
}
