package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.netcdf.NcWrite;
import org.jspecify.annotations.Nullable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

final class ChannelGroupSvAndAnglesOutputBuilder extends ChannelGroupOutputBuilder {
   private final float sampleDistance;
   private final int rangeLength;
   private final boolean writeAngles;

   ChannelGroupSvAndAnglesOutputBuilder(Group.Builder parentGroup, int frequencyIndex, PowerData powerData,
                                        float maxRange, boolean writeAngles) {
      super(parentGroup, frequencyIndex, powerData);

      sampleDistance = powerData.getSampleDistance();
      rangeLength = (int) Math.floor(maxRange / sampleDistance);
      this.writeAngles = writeAngles;

      Dimension pingTimeDim = NcWrite.addUnlimitedDimension(groupBuilder, Nc.PING_TIME);
      Dimension rangeDim = NcWrite.addDimension(groupBuilder, Nc.RANGE, rangeLength);

      NcWrite.addVariable(groupBuilder, Nc.PING_TIME, DataType.LONG, List.of(pingTimeDim))
            .addAttribute(new Attribute(CF.CALENDAR, "proleptic_gregorian"))
            .addAttribute(new Attribute(CF.UNITS, "nanoseconds since " + Instant.ofEpochMilli(referenceTimeInMillis)));
      NcWrite.addVariable(groupBuilder, Nc.RANGE, DataType.DOUBLE, List.of(rangeDim));

      NcWrite.addFloatVariable(groupBuilder, Nc.SV, List.of(pingTimeDim, rangeDim), List.of());

      if (writeAngles) {
         NcWrite.addFloatVariable(groupBuilder, Nc.ANGLE_ALONGSHIP, List.of(pingTimeDim, rangeDim), List.of());
         NcWrite.addFloatVariable(groupBuilder, Nc.ANGLE_ATHWARTSHIP, List.of(pingTimeDim, rangeDim), List.of());
      }
   }

   @Override
   ChannelGroupOutputWriter createWriter(NcChannelGroupWriter ncChannelGroupWriter) throws InvalidRangeException, IOException {
      return new ChannelGroupPulseCompressionWriter(ncChannelGroupWriter, this);
   }

   private static final class ChannelGroupPulseCompressionWriter extends ChannelGroupOutputWriter {
      private final long referenceTimeInMillis;
      private final float sampleDistance;
      private final int rangeLength;

      private final Variable pingTimeVar;

      private final Variable svVar;

      private final @Nullable Variable angleAlongshipVar;
      private final @Nullable Variable angleAthwartshipVar;

      private ChannelGroupPulseCompressionWriter(NcChannelGroupWriter ncChannelGroupWriter, ChannelGroupSvAndAnglesOutputBuilder channelGroupBuilder) throws InvalidRangeException, IOException {
         super(ncChannelGroupWriter, channelGroupBuilder);

         referenceTimeInMillis = channelGroupBuilder.referenceTimeInMillis;
         sampleDistance = channelGroupBuilder.sampleDistance;
         rangeLength = channelGroupBuilder.rangeLength;

         pingTimeVar = channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.PING_TIME);
         Variable rangeVar = channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.RANGE);

         svVar = channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.SV);

         angleAlongshipVar = channelGroupBuilder.writeAngles ? channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.ANGLE_ALONGSHIP) : null;
         angleAthwartshipVar = channelGroupBuilder.writeAngles ? channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.ANGLE_ATHWARTSHIP) : null;

         double[] ranges = new double[rangeLength];
         for (int i = 0; i < ranges.length; i++) {
            ranges[i] = i * sampleDistance;
         }
         writer.write(rangeVar, new int[]{0}, Array.makeFromJavaArray(ranges));
      }

      @Override
      void write(Ping ping, int pingTimeIndex) throws InvalidRangeException, IOException {
         PingIndex pingIndex = ping.getPingIndex();

         ncChannelGroupWriter.writeLong(pingTimeVar, (pingIndex.getTimeInMillis() - referenceTimeInMillis) * 1_000_000);

         PowerData powerData = ping.getPowerData(channel);
         if (powerData == null) {
            return;
         }

         OffsetValues resampledSv = resample(powerData, powerData.getSv());
         if (resampledSv != null) {
            float[] resampledSvValues = resampledSv.values.clone(); // Copy to not change values in the ping.
            ArrayMath.divide(resampledSvValues, PowerData.IMR_CONSTANT);
            writer.write(svVar, new int[]{pingTimeIndex, resampledSv.offset},
                  Array.makeFromJavaArray(new float[][]{resampledSvValues}));
         }

         if (angleAlongshipVar != null && angleAthwartshipVar != null) {
            AngleData angleData = powerData.getAngleData();
            if (angleData != null) {
               int count = powerData.getCount();
               float[] alongAngles = new float[count];
               float[] athwartAngles = new float[count];
               RawFileTransducer transducer = powerData.getTransducer();
               for (int i = 0; i < count; i++) {
                  alongAngles[i] = angleData.getMechanicalAlongAngle(i, transducer);
                  athwartAngles[i] = angleData.getMechanicalAthwartAngle(i, transducer);
               }
               OffsetValues resampledAlongAngles = resample(powerData, alongAngles);
               if (resampledAlongAngles != null) {
                  writer.write(angleAlongshipVar, new int[]{pingTimeIndex, resampledAlongAngles.offset},
                        Array.makeFromJavaArray(new float[][]{resampledAlongAngles.values}));
               }
               OffsetValues resampledAthwartAngles = resample(powerData, athwartAngles);
               if (resampledAthwartAngles != null) {
                  writer.write(angleAthwartshipVar, new int[]{pingTimeIndex, resampledAthwartAngles.offset},
                        Array.makeFromJavaArray(new float[][]{resampledAthwartAngles.values}));
               }
            }
         }
      }

      private @Nullable OffsetValues resample(PowerData powerData, float[] values) {
         return OffsetValues.resample(values, powerData, sampleDistance, rangeLength);
      }
   }
}
