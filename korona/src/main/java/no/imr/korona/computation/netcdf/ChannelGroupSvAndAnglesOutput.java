package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.netcdf.NcBuild;
import no.imr.tools.netcdf.NcWrite;
import no.imr.tools.netcdf.NetcdfUtils;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

final class ChannelGroupSvAndAnglesOutput implements PingByPingOutput {
   private final int channel;
   private final RangeConfig rangeConfig;
   private final boolean writeAngles;

   ChannelGroupSvAndAnglesOutput(PowerData powerData, float maxRange, boolean writeAngles) {
      channel = powerData.getChannel();

      float sampleDistance = powerData.getSampleDistance();
      int rangeLength = (int) Math.floor(maxRange / sampleDistance);
      rangeConfig = new RangeConfig(0, sampleDistance, rangeLength);
      this.writeAngles = writeAngles;
   }

   @Override
   public PingByPingBuilder createBuilder(Group.Builder groupBuilder, NcConfig ncConfig) {
      Dimension pingTimeDim = NcBuild.addUnlimitedDimension(groupBuilder, Nc.PING_TIME);
      Dimension rangeDim = NcBuild.addDimension(groupBuilder, Nc.RANGE, rangeConfig.length());

      groupBuilder.addVariable(NcBuild.timeVariable(Nc.PING_TIME, List.of(pingTimeDim), ncConfig.referenceTime));
      groupBuilder.addVariable(NcBuild.doubleVariable(Nc.RANGE, List.of(rangeDim)));

      groupBuilder.addVariable(NcBuild.floatVariable(Nc.SV, List.of(pingTimeDim, rangeDim)));

      if (writeAngles) {
         groupBuilder.addVariable(NcBuild.floatVariable(Nc.ANGLE_ALONGSHIP, List.of(pingTimeDim, rangeDim)));
         groupBuilder.addVariable(NcBuild.floatVariable(Nc.ANGLE_ATHWARTSHIP, List.of(pingTimeDim, rangeDim)));
      }

      return (writer, group) -> {
         writeUpFront(writer, group);
         List<PingByPingWriter> writers = new ArrayList<>();
         writers.add(svWriter(writer, group, ncConfig));
         if (writeAngles) {
            writers.add(anglesWriter(writer, group));
         }
         return PingByPingWriter.of(writers);
      };
   }

   private void writeUpFront(NetcdfFormatWriter writer, Group group) throws InvalidRangeException, IOException {
      writer.write(NetcdfUtils.findVariable(group, Nc.RANGE), rangeConfig.toArray());
   }

   private PingByPingWriter svWriter(NetcdfFormatWriter writer, Group group, NcConfig ncConfig) throws IOException {
      Variable pingTimeVar = NetcdfUtils.findVariable(group, Nc.PING_TIME);

      Variable svVar = NetcdfUtils.findVariable(group, Nc.SV);

      return (ping, pingTimeIndex) -> {
         NcWrite.longD1(writer, pingTimeVar, pingTimeIndex, ncConfig.referenceTime.until(ping.getInstant(), ChronoUnit.NANOS));

         PowerData powerData = ping.getPowerData(channel);
         if (powerData == null) {
            return;
         }

         OffsetValues resampledSv = rangeConfig.resample(powerData, powerData.getSv());
         if (resampledSv != null) {
            float[] resampledSvValues = resampledSv.values().clone(); // Copy to not change values in the ping.
            ArrayMath.divide(resampledSvValues, PowerData.IMR_CONSTANT);
            NcWrite.floatD2(writer, svVar, pingTimeIndex, resampledSv.offset(), resampledSvValues);
         }
      };
   }

   private PingByPingWriter anglesWriter(NetcdfFormatWriter writer, Group group) throws IOException {
      Variable angleAlongshipVar = NetcdfUtils.findVariable(group, Nc.ANGLE_ALONGSHIP);
      Variable angleAthwartshipVar = NetcdfUtils.findVariable(group, Nc.ANGLE_ATHWARTSHIP);

      return (ping, pingTimeIndex) -> {
         PowerData powerData = ping.getPowerData(channel);
         if (powerData == null) {
            return;
         }
         AngleData angleData = powerData.getAngleData();
         if (angleData == null) {
            return;
         }
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
            NcWrite.floatD2(writer, angleAlongshipVar, pingTimeIndex, resampledAlongAngles.offset(), resampledAlongAngles.values());
         }
         OffsetValues resampledAthwartAngles = rangeConfig.resample(powerData, athwartAngles);
         if (resampledAthwartAngles != null) {
            NcWrite.floatD2(writer, angleAthwartshipVar, pingTimeIndex, resampledAthwartAngles.offset(), resampledAthwartAngles.values());
         }
      };
   }
}
