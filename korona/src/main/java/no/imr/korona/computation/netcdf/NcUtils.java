package no.imr.korona.computation.netcdf;

import no.imr.korona.Korona;
import no.imr.korona.computation.offset.TransducerParameters;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.Utils;
import no.imr.tools.netcdf.NcWrite;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

final class NcUtils {
   private NcUtils() {
   }

   static NetcdfFormatWriter.Builder newBuilder(Path file) {
      NetcdfFormatWriter.Builder builder = NcWrite.newBuilder(file);

      builder
            .addAttribute(new Attribute("name", "KORONA"))
            .addAttribute(new Attribute("description", "Multi-frequency echosounder data"))
            .addAttribute(new Attribute("time", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()))
            .addAttribute(new Attribute("version", Korona.VERSION))
            .addAttribute(new Attribute("git_commit", Utils.GIT_COMMIT));

      return builder;
   }

   static void addTransducerOffsetVariables(Group.Builder builder, Dimension frequencyDim, NcOptionalConfig optionalConfig) {
      if (optionalConfig.horizontalTransducerParameterManager != null) {
         NcWrite.addFloatVariable(builder, Nc.TRANSDUCER_OFFSET_ALONGSHIP, List.of(frequencyDim), List.of(Nc.CHANNEL_ID));
         NcWrite.addFloatVariable(builder, Nc.TRANSDUCER_OFFSET_ATHWARTSHIP, List.of(frequencyDim), List.of(Nc.CHANNEL_ID));
      }
      if (optionalConfig.verticalTransducerParameterManager != null) {
         NcWrite.addFloatVariable(builder, Nc.TRANSDUCER_OFFSET_VERTICAL, List.of(frequencyDim), List.of(Nc.CHANNEL_ID));
      }
   }

   static void writeTransducerOffsets(NetcdfFormatWriter writer, List<RawFileTransducer> transducers, NcOptionalConfig optionalConfig) throws IOException, InvalidRangeException {
      if (optionalConfig.horizontalTransducerParameterManager != null) {
         float[] transducerOffsetAlongship = new float[transducers.size()];
         float[] transducerOffsetAthwartship = new float[transducers.size()];
         for (int channelIndex = 0; channelIndex < transducers.size(); channelIndex++) {
            int kHz = transducers.get(channelIndex).getKHz();
            TransducerParameters par = optionalConfig.horizontalTransducerParameterManager.getTransducerOffsetPar(kHz).orElse(null);
            transducerOffsetAlongship[channelIndex] = par != null ? par.alongCorrection.getFloatValue() : Float.NaN;
            transducerOffsetAthwartship[channelIndex] = par != null ? par.athwartCorrection.getFloatValue() : Float.NaN;
         }
         writer.write(Nc.TRANSDUCER_OFFSET_ALONGSHIP, Array.makeFromJavaArray(transducerOffsetAlongship));
         writer.write(Nc.TRANSDUCER_OFFSET_ATHWARTSHIP, Array.makeFromJavaArray(transducerOffsetAthwartship));
      }
      if (optionalConfig.verticalTransducerParameterManager != null) {
         float[] transducerOffsetVertical = new float[transducers.size()];
         for (int channelIndex = 0; channelIndex < transducers.size(); channelIndex++) {
            int kHz = transducers.get(channelIndex).getKHz();
            TransducerParameters par = optionalConfig.verticalTransducerParameterManager.getTransducerOffsetPar(kHz).orElse(null);
            transducerOffsetVertical[channelIndex] = par != null
                  ? par.getDeltaZ0() + par.getDeltaZPulseDelay()
                  : Float.NaN;
         }
         writer.write(Nc.TRANSDUCER_OFFSET_VERTICAL, Array.makeFromJavaArray(transducerOffsetVertical));
      }
   }
}
