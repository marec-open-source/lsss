package no.imr.korona.computation.netcdf;

import no.imr.korona.computation.broadband.BroadbandSvByFrequency;
import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.netcdf.NcBuild;
import no.imr.tools.netcdf.NcWrite;
import no.imr.tools.netcdf.NetcdfUtils;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class ChannelGroupBroadbandSvOutput implements ChannelGroupOutput {
   private final @Nullable Float optionalDeltaRange;
   private final @Nullable Float optionalMaxRange;
   private final float fftWindowSizeInPulseLengths;
   private final float deltaFrequency;
   private final boolean writeAngles;

   ChannelGroupBroadbandSvOutput(@Nullable Float optionalDeltaRange, @Nullable Float optionalMaxRange,
                                 float fftWindowSizeInPulseLengths, float deltaFrequency,
                                 boolean writeAngles) {
      this.optionalDeltaRange = optionalDeltaRange;
      this.optionalMaxRange = optionalMaxRange;
      this.fftWindowSizeInPulseLengths = fftWindowSizeInPulseLengths;
      this.deltaFrequency = deltaFrequency;
      this.writeAngles = writeAngles;
   }

   @Override
   public PingByPingBuilder createBuilder(Group.Builder groupBuilder, NcConfig ncConfig, ChannelData channelData) {
      float deltaRange = optionalDeltaRange != null ? optionalDeltaRange : channelData.getSampleDistance();
      float maxRange = optionalMaxRange != null ? optionalMaxRange : channelData.getMaxRange();
      if (channelData instanceof BroadbandData broadbandData) {
         return new ChannelGroupBroadbandSvOutputBuilder(groupBuilder, ncConfig, broadbandData, deltaRange, maxRange, writeAngles, fftWindowSizeInPulseLengths, deltaFrequency);
      }
      return new ChannelGroupSvAndAnglesOutput(channelData.getPowerData(), maxRange, writeAngles).createBuilder(groupBuilder, ncConfig);
   }

   private static final class ChannelGroupBroadbandSvOutputBuilder implements PingByPingBuilder {
      private final int channel;
      private final FloatRange gridFrequencyRange;
      private final int gridFrequencyLength;
      private final float fftWindowSizeInPulseLengths;
      private final RangeConfig rangeConfig;
      private final boolean writeAngles;
      private final PingByPingBuilder broadbandInfoBuilder;

      private ChannelGroupBroadbandSvOutputBuilder(Group.Builder groupBuilder, NcConfig ncConfig, BroadbandData broadbandData,
                                                   float deltaRange, float maxRange, boolean writeAngles,
                                                   float fftWindowSizeInPulseLengths, float deltaFrequency) {
         channel = broadbandData.getChannel();

         gridFrequencyRange = broadbandData.getFrequencyRange().roundToMultipleOf(deltaFrequency);
         gridFrequencyLength = Math.round(gridFrequencyRange.getSize() / deltaFrequency) + 1;
         this.fftWindowSizeInPulseLengths = fftWindowSizeInPulseLengths;

         int rangeLength = (int) Math.floor(maxRange / deltaRange);
         rangeConfig = new RangeConfig(0, deltaRange, rangeLength);
         this.writeAngles = writeAngles;

         broadbandInfoBuilder = new BroadbandInfoOutput(broadbandData).createBuilder(groupBuilder, ncConfig);
         Dimension pingTimeDim = groupBuilder.findDimensionLocal(Nc.PING_TIME).orElseThrow();
         Dimension rangeDim = NcBuild.addDimension(groupBuilder, Nc.RANGE, rangeLength);
         Dimension broadbandFrequencyDim = NcBuild.addDimension(groupBuilder, Nc.BROADBAND_FREQUENCY, gridFrequencyLength);

         groupBuilder.addVariable(NcBuild.doubleVariable(Nc.RANGE, List.of(rangeDim)));

         groupBuilder.addVariable(NcBuild.floatVariable(Nc.BROADBAND_FREQUENCY, List.of(broadbandFrequencyDim)));
         groupBuilder.addVariable(NcBuild.floatVariable(Nc.BROADBAND_SV, List.of(pingTimeDim, rangeDim, broadbandFrequencyDim)));

         if (writeAngles) {
            groupBuilder.addVariable(NcBuild.floatVariable(Nc.ANGLE_ALONGSHIP, List.of(pingTimeDim, rangeDim)));
            groupBuilder.addVariable(NcBuild.floatVariable(Nc.ANGLE_ATHWARTSHIP, List.of(pingTimeDim, rangeDim)));
         }
      }

      @Override
      public PingByPingWriter createWriter(NetcdfFormatWriter writer, Group group) throws InvalidRangeException, IOException {
         writeUpFront(writer, group);
         List<PingByPingWriter> writers = new ArrayList<>();
         writers.add(broadbandInfoBuilder.createWriter(writer, group));
         writers.add(broadbandSvWriter(writer, group));
         if (writeAngles) {
            writers.add(anglesWriter(writer, group));
         }
         return PingByPingWriter.of(writers);
      }

      private void writeUpFront(NetcdfFormatWriter writer, Group group) throws InvalidRangeException, IOException {
         writer.write(NetcdfUtils.findVariable(group, Nc.RANGE), rangeConfig.toArray());

         float[] broadbandFrequencies = new float[gridFrequencyLength];
         float deltaFrequency = gridFrequencyRange.getSize() / (gridFrequencyLength - 1);
         for (int i = 0; i < broadbandFrequencies.length; i++) {
            broadbandFrequencies[i] = gridFrequencyRange.min() + i * deltaFrequency;
         }
         writer.write(NetcdfUtils.findVariable(group, Nc.BROADBAND_FREQUENCY), Array.makeFromJavaArray(broadbandFrequencies));
      }

      private PingByPingWriter broadbandSvWriter(NetcdfFormatWriter writer, Group group) throws IOException {
         Variable broadbandSvVar = NetcdfUtils.findVariable(group, Nc.BROADBAND_SV);

         return (ping, pingTimeIndex) -> {
            BroadbandData broadbandData = ping.getBroadbandData(channel);
            if (broadbandData == null) {
               return;
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
               OffsetValues resampledSv = OffsetValues.resample(sv, frequencyRange, gridFrequencyRange, gridFrequencyLength);
               if (resampledSv != null) {
                  ArrayMath.divide(resampledSv.values(), PowerData.IMR_CONSTANT);
                  NcWrite.floatD3(writer, broadbandSvVar, pingTimeIndex, iRange, resampledSv.offset(), resampledSv.values());
               }
            }
         };
      }

      private PingByPingWriter anglesWriter(NetcdfFormatWriter writer, Group group) throws IOException {
         Variable angleAlongshipVar = NetcdfUtils.findVariable(group, Nc.ANGLE_ALONGSHIP);
         Variable angleAthwartshipVar = NetcdfUtils.findVariable(group, Nc.ANGLE_ATHWARTSHIP);

         float[] angleAlongshipValues = new float[rangeConfig.length()];
         float[] angleAthwartshipValues = new float[rangeConfig.length()];

         return (ping, pingTimeIndex) -> {
            BroadbandData broadbandData = ping.getBroadbandData(channel);
            if (broadbandData == null) {
               return;
            }
            AngleData angleData = broadbandData.getAngleData();
            if (angleData == null) {
               return;
            }
            RawFileTransducer transducer = broadbandData.getTransducer();

            for (int iRange = 0; iRange < rangeConfig.length(); iRange++) {
               float centerRange = rangeConfig.centerRange(iRange);
               int i = broadbandData.rangeToContainingSampleIndex(centerRange);
               if (i >= 0 && i < broadbandData.getCount()) {
                  angleAlongshipValues[iRange] = angleData.getMechanicalAlongAngle(i, transducer);
                  angleAthwartshipValues[iRange] = angleData.getMechanicalAthwartAngle(i, transducer);
               } else {
                  angleAlongshipValues[iRange] = Float.NaN;
                  angleAthwartshipValues[iRange] = Float.NaN;
               }
            }
            NcWrite.floatD2(writer, angleAlongshipVar, pingTimeIndex, 0, angleAlongshipValues);
            NcWrite.floatD2(writer, angleAthwartshipVar, pingTimeIndex, 0, angleAthwartshipValues);
         };
      }
   }
}
