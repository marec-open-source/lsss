package no.imr.korona.computation.noise;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.SimplePingModule;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.data.datagrams.Nqp0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.RangeParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class NoiseAcceptanceModule extends SimplePingModule {
   public final RangeParameter noiseFraction = new RangeParameter(
         new Name("NoiseFraction", "Noise fraction range"),
         0.25f, 4, Unit.DIMENSIONLESS, ValueConstraints.gteLte(0.1f, 10f),
         "The fraction of detected noise to noise on file should lie within this range");

   public final BooleanParameter useFileNoiseFile = new BooleanParameter(
         new Name("UseFileNoiseFile", "Use file noise file"),
         true,
         "Check the noise against file noise file, if available");

   public final BooleanParameter useDayNoiseFile = new BooleanParameter(
         new Name("UseDayNoiseFile", "Use day noise file"),
         true,
         "Check the noise against day noise file, if available");

   public final BooleanParameter useSurveyNoiseFile = new BooleanParameter(
         new Name("UseSurveyNoiseFile", "Use survey noise file"),
         true,
         "Check the noise against survey noise file, if available");

   public final BooleanParameter useSurveyLowNoiseFile = new BooleanParameter(
         new Name("UseSurveyLowNoiseFile", "Use survey low noise file"),
         true,
         "Check the noise against survey low-median noise file, if available");

   // Not sure if this  should be available  (RK, 2015.02.26):
   //public final BooleanParameter useSurveyHighNoiseFile = new BooleanParameter(
   //      new Name("UseSurveyHighNoiseFile", "Use survey high noise file"),
   //      true,
   //      "Check the noise against survey high-median noise file, if available");

   public NoiseAcceptanceModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            noiseFraction,
            useFileNoiseFile,
            useDayNoiseFile,
            useSurveyNoiseFile,
            useSurveyLowNoiseFile
      );
   }

   @Override
   public SimplePingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new NoiseAcceptanceModuleComputation(this, computationContext, pingSource);
   }

   private static final class NoiseAcceptanceModuleComputation extends SimplePingModuleComputation {
      private final NoiseAcceptanceModule module;
      private final List<NoiseFileChecker> noiseFileHierarchy = new ArrayList<>();
      private final int channelsIn;

      private NoiseAcceptanceModuleComputation(NoiseAcceptanceModule module, ComputationContext computationContext, PingSource pingSource) throws IOException {
         super(module, computationContext, pingSource);

         this.module = module;
         RawFileConfiguration rawFileConfiguration = getPingConfiguration().getRawFileConfiguration();
         Path koronaDirectory = computationContext.getAssociatedKoronaDirectory();
         if (koronaDirectory != null) {
            Path noiseDir = koronaDirectory.resolve(NoiseUtils.MEDIAN_NOISE_SUBFOLDER);
            if (module.useFileNoiseFile.getBooleanValue()) {
               NoiseFileChecker checker = makeFileNoiseFileChecker(noiseDir, rawFileConfiguration);
               if (checker != null) {
                  noiseFileHierarchy.add(checker);
               }
            }
            if (module.useDayNoiseFile.getBooleanValue()) {
               NoiseFileChecker checker = makeDayNoiseFileChecker(noiseDir, rawFileConfiguration);
               if (checker != null) {
                  noiseFileHierarchy.add(checker);
               }
            }
            if (module.useSurveyNoiseFile.getBooleanValue()) {
               NoiseFileChecker checker = makeSurveyNoiseFileChecker(noiseDir);
               if (checker != null) {
                  noiseFileHierarchy.add(checker);
               }
            }
            if (module.useSurveyLowNoiseFile.getBooleanValue()) {
               NoiseFileChecker checker = makeSurveyLowNoiseFileChecker(noiseDir);
               if (checker != null) {
                  noiseFileHierarchy.add(checker);
               }
            }
            /*
            if (useSurveyHighNoiseFile.getBooleanValue()) {
               NoiseFileChecker checker = makeSurveyHighNoiseFileChecker(noiseDir);
               if (checker != null) {
                  noiseFileHierarchy.add(checker);
               }
            }
            */
         }

         channelsIn = rawFileConfiguration.getTransducerCount();
      }

      private boolean isValidChannel(int channel) {
         return channel >= 1 && channel <= channelsIn;
      }

      @Override
      protected void processPing(Ping ping) {
         if (noiseFileHierarchy.isEmpty()) {
            // Do nothing, pass the NQP0 datagrams through unchecked.
            return;
         }

         // Get datagrams as list before modifying ping.
         List<Nqp0Datagram> nqp0Datagrams = ping.getPingItems(Nqp0Datagram.class).toList();
         nqp0Datagrams.forEach(nqp -> {
            int channel = nqp.getChannel();
            if (isValidChannel(channel)) {
               float ne = nqp.getAverage();

               for (NoiseFileChecker noiseFileChecker : noiseFileHierarchy) {
                  if (noiseFileChecker.hasNoise(channel)) {
                     if (!noiseFileChecker.isInside(ne, channel, module.noiseFraction)) {
                        ne = noiseFileChecker.getNe(channel);
                        float nh = noiseFileChecker.getNh(channel);
                        // Create a new NQP0 datagram with Ne and Nh values from file.
                        Nqp0Datagram nqpFromFile = new Nqp0Datagram(nqp.getInstant(), (short) channel,
                              ne, nh, nqp.getQuality());
                        ping.remove(nqp);
                        ping.add(nqpFromFile);
                     }
                     break;
                  }
               }
            }
         });
      }
   }

   private static @Nullable NoiseFileChecker makeSurveyNoiseFileChecker(Path noiseDir) {
      Path file = NoiseUtils.getSurveyNoiseFile(noiseDir);
      if (Files.exists(file)) {
         return new NoiseFileChecker("Survey", file);
      }
      return null;
   }

   private static @Nullable NoiseFileChecker makeSurveyLowNoiseFileChecker(Path noiseDir) {
      Path file = NoiseUtils.getSurveyLowNoiseFile(noiseDir);
      if (Files.exists(file)) {
         return new NoiseFileChecker("Survey (low)", file);
      }
      return null;
   }

   private static @Nullable NoiseFileChecker makeSurveyHighNoiseFileChecker(Path noiseDir) {
      Path file = NoiseUtils.getSurveyHighNoiseFile(noiseDir);
      if (Files.exists(file)) {
         return new NoiseFileChecker("Survey (high)", file);
      }
      return null;
   }

   private static @Nullable NoiseFileChecker makeDayNoiseFileChecker(Path noiseDir, RawFileConfiguration rawFileConfiguration) {
      Path file = NoiseUtils.getDayNoiseFile(noiseDir, rawFileConfiguration);
      if (Files.exists(file)) {
         return new NoiseFileChecker("Day", file);
      }
      return null;
   }

   private static @Nullable NoiseFileChecker makeFileNoiseFileChecker(Path noiseDir, RawFileConfiguration rawFileConfiguration) throws IOException {
      // Use the noise file closest in time before the current time
      List<Path> files = NoiseUtils.listNoiseFiles(noiseDir.resolve(NoiseUtils.FILE_NOISE_FILE_PREFIX), NoiseUtils.surveyFilePredicate());
      Path closestFile = NoiseUtils.findClosestNoiseFileBefore(rawFileConfiguration.getInstant(), files);
      if (closestFile != null) {
         return new NoiseFileChecker("File", closestFile);
      }
      return null;
   }

   private static final class NoiseFileChecker {
      private final String name;
      private final PerChannelNoiseFile perChannelNoiseFile;

      private NoiseFileChecker(String name, Path file) {
         this.name = name;
         perChannelNoiseFile = new PerChannelNoiseFile(file);
      }

      private float getNe(int channel) {
         return perChannelNoiseFile.getNe(channel);
      }

      private float getNh(int channel) {
         return perChannelNoiseFile.getNh(channel);
      }

      private boolean hasNoise(int channel) {
         float ne = perChannelNoiseFile.getNe(channel);
         // A value of 0 means that the value is not set
         return ne != 0;
      }

      private boolean isInside(float ne, int channel, RangeParameter rangeParameter) {
         float fileNe = perChannelNoiseFile.getNe(channel);
         float fraction = ne / fileNe;
         return rangeParameter.getValue().containsIncludingEnd(fraction);
      }

      @Override
      public String toString() {
         return name;
      }
   }
}
