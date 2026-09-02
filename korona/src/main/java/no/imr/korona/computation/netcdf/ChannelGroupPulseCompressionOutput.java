package no.imr.korona.computation.netcdf;

import no.imr.korona.computation.broadband.PulseCompressionConfig;
import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.netcdf.NcBuild;
import no.imr.tools.netcdf.NcWrite;
import no.imr.tools.netcdf.NetcdfUtils;
import org.jspecify.annotations.Nullable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

final class ChannelGroupPulseCompressionOutput implements ChannelGroupOutput {
   private final @Nullable Float optionalMaxRange;
   private final boolean writeAngles;

   ChannelGroupPulseCompressionOutput(@Nullable Float optionalMaxRange, boolean writeAngles) {
      this.optionalMaxRange = optionalMaxRange;
      this.writeAngles = writeAngles;
   }

   @Override
   public PingByPingBuilder createBuilder(Group.Builder groupBuilder, NcConfig ncConfig, ChannelData channelData) {
      float maxRange = optionalMaxRange != null ? optionalMaxRange : channelData.getMaxRange();
      if (channelData instanceof BroadbandData broadbandData) {
         return new ChannelGroupPulseCompressionOutputBuilder(groupBuilder, ncConfig, broadbandData, maxRange, writeAngles);
      }
      return new ChannelGroupSvAndAnglesOutput(channelData.getPowerData(), maxRange, writeAngles).createBuilder(groupBuilder, ncConfig);
   }

   private static final class ChannelGroupPulseCompressionOutputBuilder implements PingByPingBuilder {
      private final int channel;
      private final int sectorLength;
      private final RangeConfig rangeConfig;
      private final boolean writeAngles;
      private final PulseCompressionConfig pulseCompressionConfig;
      private final PingByPingBuilder broadbandInfoBuilder;

      private ChannelGroupPulseCompressionOutputBuilder(Group.Builder groupBuilder, NcConfig ncConfig, BroadbandData broadbandData,
                                                        float maxRange, boolean writeAngles) {
         channel = broadbandData.getChannel();

         sectorLength = broadbandData.getSectorCount();
         float minRange = broadbandData.getMinRange();
         float sampleDistance = broadbandData.getSampleDistance();
         int rangeLength = (int) Math.floor(maxRange / sampleDistance);
         rangeConfig = new RangeConfig(minRange, sampleDistance, rangeLength);
         this.writeAngles = writeAngles;

         pulseCompressionConfig = broadbandData.getPulseCompression().getConfig();

         broadbandInfoBuilder = new BroadbandInfoOutput(broadbandData).createBuilder(groupBuilder, ncConfig);
         Dimension pingTimeDim = groupBuilder.findDimensionLocal(Nc.PING_TIME).orElseThrow();
         Dimension rangeDim = NcBuild.addDimension(groupBuilder, Nc.RANGE, rangeLength);
         Dimension sectorDim = NcBuild.addDimension(groupBuilder, Nc.SECTOR, sectorLength);

         groupBuilder.addVariable(NcBuild.newVariable(Nc.SECTOR, DataType.INT, List.of(sectorDim)));
         groupBuilder.addVariable(NcBuild.doubleVariable(Nc.RANGE, List.of(rangeDim)));

         groupBuilder.addVariable(NcBuild.floatVariable(Nc.PULSE_COMPRESSED_RE, List.of(pingTimeDim, sectorDim, rangeDim)));
         groupBuilder.addVariable(NcBuild.floatVariable(Nc.PULSE_COMPRESSED_IM, List.of(pingTimeDim, sectorDim, rangeDim)));

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
         writers.add(pulseCompressedWriter(writer, group));
         if (writeAngles) {
            writers.add(anglesWriter(writer, group));
         }
         return PingByPingWriter.of(writers);
      }

      private void writeUpFront(NetcdfFormatWriter writer, Group group) throws InvalidRangeException, IOException {
         int[] sectors = IntStream.rangeClosed(1, sectorLength).toArray();
         writer.write(NetcdfUtils.findVariable(group, Nc.SECTOR), Array.makeFromJavaArray(sectors));

         writer.write(NetcdfUtils.findVariable(group, Nc.RANGE), rangeConfig.toArray());
      }

      private PingByPingWriter pulseCompressedWriter(NetcdfFormatWriter writer, Group group) throws IOException {
         Variable pulseCompressedReVar = NetcdfUtils.findVariable(group, Nc.PULSE_COMPRESSED_RE);
         Variable pulseCompressedImVar = NetcdfUtils.findVariable(group, Nc.PULSE_COMPRESSED_IM);

         float[] floatValues = new float[rangeConfig.length()];

         return (ping, pingTimeIndex) -> {
            BroadbandData broadbandData = ping.getBroadbandData(channel);
            if (broadbandData == null) {
               return;
            }

            if (!pulseCompressionConfig.equals(broadbandData.getPulseCompression().getConfig())) {
               throw new IOException("Pulse compression config changed for channel " + broadbandData.getTransducer().getChannelId()
                     + " at " + ping.getInstant());
            }

            int n = Math.min(floatValues.length, broadbandData.getCount());
            if (n < floatValues.length) {
               Arrays.fill(floatValues, n, floatValues.length, Float.NaN);
            }

            for (int sectorIndex = 0; sectorIndex < broadbandData.getSectorCount(); sectorIndex++) {
               ComplexArray sectorData = ComplexArray.of(broadbandData.getReal()[sectorIndex], broadbandData.getImag()[sectorIndex]);
               ComplexArray pc = broadbandData.getPulseCompression().computePulseCompressedSignal(sectorData);

               for (int i = 0; i < n; i++) {
                  floatValues[i] = (float) pc.re(i);
               }
               NcWrite.floatD3(writer, pulseCompressedReVar, pingTimeIndex, sectorIndex, 0, floatValues);

               for (int i = 0; i < n; i++) {
                  floatValues[i] = (float) pc.im(i);
               }
               NcWrite.floatD3(writer, pulseCompressedImVar, pingTimeIndex, sectorIndex, 0, floatValues);
            }
         };
      }

      private PingByPingWriter anglesWriter(NetcdfFormatWriter writer, Group group) throws IOException {
         Variable angleAlongshipVar = NetcdfUtils.findVariable(group, Nc.ANGLE_ALONGSHIP);
         Variable angleAthwartshipVar = NetcdfUtils.findVariable(group, Nc.ANGLE_ATHWARTSHIP);

         float[] floatValues = new float[rangeConfig.length()];

         return (ping, pingTimeIndex) -> {
            BroadbandData broadbandData = ping.getBroadbandData(channel);
            if (broadbandData == null) {
               return;
            }
            AngleData angleData = broadbandData.getAngleData();
            if (angleData == null) {
               return;
            }

            int n = Math.min(floatValues.length, broadbandData.getCount());
            if (n < floatValues.length) {
               Arrays.fill(floatValues, n, floatValues.length, Float.NaN);
            }

            RawFileTransducer transducer = broadbandData.getTransducer();

            for (int i = 0; i < n; i++) {
               floatValues[i] = angleData.getMechanicalAlongAngle(i, transducer);
            }
            NcWrite.floatD2(writer, angleAlongshipVar, pingTimeIndex, 0, floatValues);

            for (int i = 0; i < n; i++) {
               floatValues[i] = angleData.getMechanicalAthwartAngle(i, transducer);
            }
            NcWrite.floatD2(writer, angleAthwartshipVar, pingTimeIndex, 0, floatValues);
         };
      }
   }
}
