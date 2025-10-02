package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.netcdf.NcWrite;
import org.jspecify.annotations.Nullable;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.util.List;

final class GridSvAndAnglesOutput extends GridOutput {
   private final boolean writeAngles;

   GridSvAndAnglesOutput(boolean writeAngles) {
      this.writeAngles = writeAngles;
   }

   @Override
   void addVariables(Group.Builder builder, Dimension frequencyDim, Dimension pingTimeDim, Dimension rangeDim) {
      List<String> coordinates = List.of(Nc.CHANNEL_ID, Nc.RAW_FILE, Nc.LATITUDE, Nc.LONGITUDE);
      NcWrite.addFloatVariable(builder, Nc.SV, List.of(frequencyDim, pingTimeDim, rangeDim), coordinates);
      if (writeAngles) {
         NcWrite.addFloatVariable(builder, Nc.ANGLE_ALONGSHIP, List.of(frequencyDim, pingTimeDim, rangeDim), coordinates);
         NcWrite.addFloatVariable(builder, Nc.ANGLE_ATHWARTSHIP, List.of(frequencyDim, pingTimeDim, rangeDim), coordinates);
      }
   }

   @Override
   GridOutputWriter createWriter(NcGridWriter ncGridWriter) {
      return new GridSvAndAnglesWriter(ncGridWriter, writeAngles);
   }

   private static final class GridSvAndAnglesWriter extends GridOutputWriter {
      private final NcGridWriter ncGridWriter;
      private final NetcdfFormatWriter writer;
      private final int[] channelIndexToNetcdfFrequencyIndex;
      private final int channelCount;

      private final Variable svVar;
      private final @Nullable Variable angleAlongshipVar;
      private final @Nullable Variable angleAthwartshipVar;

      private GridSvAndAnglesWriter(NcGridWriter ncGridWriter, boolean writeAngles) {
         this.ncGridWriter = ncGridWriter;
         writer = ncGridWriter.getWriter();
         channelIndexToNetcdfFrequencyIndex = ncGridWriter.getChannelIndexToNetcdfFrequencyIndex();
         channelCount = channelIndexToNetcdfFrequencyIndex.length;

         svVar = ncGridWriter.findVariable(Nc.SV);
         angleAlongshipVar = writeAngles ? ncGridWriter.findVariable(Nc.ANGLE_ALONGSHIP) : null;
         angleAthwartshipVar = writeAngles ? ncGridWriter.findVariable(Nc.ANGLE_ATHWARTSHIP) : null;
      }

      @Override
      void write(Ping ping, int pingTimeIndex) throws InvalidRangeException, IOException {
         for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
            PowerData powerData = ping.getPowerData(channelIndex + 1);
            if (powerData == null) {
               continue;
            }
            int frequencyIndex = channelIndexToNetcdfFrequencyIndex[channelIndex];

            OffsetValues resampledSv = ncGridWriter.resample(powerData, powerData.getSv());
            if (resampledSv != null) {
               float[] resampledSvValues = resampledSv.values.clone(); // Copy to not change values in the ping.
               ArrayMath.divide(resampledSvValues, PowerData.IMR_CONSTANT);
               writer.write(svVar, new int[]{frequencyIndex, pingTimeIndex, resampledSv.offset},
                     Array.makeFromJavaArray(new float[][][]{{resampledSvValues}}));
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
                  OffsetValues resampledAlongAngles = ncGridWriter.resample(powerData, alongAngles);
                  if (resampledAlongAngles != null) {
                     writer.write(angleAlongshipVar, new int[]{frequencyIndex, pingTimeIndex, resampledAlongAngles.offset},
                           Array.makeFromJavaArray(new float[][][]{{resampledAlongAngles.values}}));
                  }
                  OffsetValues resampledAthwartAngles = ncGridWriter.resample(powerData, athwartAngles);
                  if (resampledAthwartAngles != null) {
                     writer.write(angleAthwartshipVar, new int[]{frequencyIndex, pingTimeIndex, resampledAthwartAngles.offset},
                           Array.makeFromJavaArray(new float[][][]{{resampledAthwartAngles.values}}));
                  }
               }
            }
         }
      }
   }
}
