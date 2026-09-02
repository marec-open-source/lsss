package no.imr.korona.computation.noise;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.datagrams.Nqp0Datagram;
import no.imr.korona.data.datagrams.subdatagrams.plot.PlotParameterUtils;
import no.imr.korona.data.datagrams.subdatagrams.plot.pojo.PlotParameterConfig;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;

final class NoiseQuantificationModuleComputation extends GeneralPingModuleComputation {
   private static final float LOW_THRESHOLD = 1e-15f;

   private static final Name NOISE_DATA_CATEGORY = new Name("noiseDataCategory", "Noise data category");
   private static final Name NOISE_HISTOGRAM_CELL_COUNT = new Name("noiseHistogramCellCount", "Noise histogram cell count");
   private static final Name NOISE_HISTOGRAM_SAMPLE_COUNT = new Name("noiseHistogramSampleCount", "Noise histogram sample count");

   private final NoiseQuantificationModule module;
   private final Queue<Ping> outputQueue = new ArrayDeque<>();
   private final BiFunction<Ping, Integer, RangeValues> rangeFunction;
   private final List<List<BaseNoiseMask>> noiseMasks;
   private final BaseHistogram[] histograms;
   private final List<Ping> timeStepBuffer;
   private final int[] timeStepBufferIndices;
   private final Set<Integer> selectedFallbackNoiseChannels;
   private final NoiseFile noiseFile;

   NoiseQuantificationModuleComputation(NoiseQuantificationModule module, ComputationContext computationContext, PingSource pingSource) {
      super(module, computationContext, pingSource);

      this.module = module;
      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      rangeFunction = NoiseQuantificationModule.rangeFunctionForPingConfiguration(pingConfiguration);
      if (module.writePlotParameters.getBooleanValue()) {
         PlotParameterUtils.getOrCreateConfigSubDatagram(pingConfiguration)
               .addConfig(new PlotParameterConfig(NOISE_DATA_CATEGORY, Unit.NONE, true))
               .addConfig(new PlotParameterConfig(NOISE_HISTOGRAM_CELL_COUNT, Unit.COUNT, true))
               .addConfig(new PlotParameterConfig(NOISE_HISTOGRAM_SAMPLE_COUNT, Unit.COUNT, true));
      }

      RawFileConfiguration rawFileConfiguration = pingConfiguration.getRawFileConfiguration();
      int n = rawFileConfiguration.getTransducerCount();

      // Must allocate N+1 because channel numbering starts at 1.
      noiseMasks = new ArrayList<>(n + 1);
      for (int i = 0; i < n + 1; i++) {
         noiseMasks.add(new ArrayList<>());
      }
      histograms = new BaseHistogram[n + 1];

      timeStepBuffer = new ArrayList<>();
      timeStepBufferIndices = new int[n + 1];

      noiseFile = createNoiseFile(rawFileConfiguration);

      for (int channel = 1; channel <= n; channel++) {
         int khz = rawFileConfiguration.getTransducers().get(channel - 1).getKHz();

         noiseMasks.get(channel).add(new LowLevelNoiseMask(LOW_THRESHOLD));

         BaseNoiseMask smoother = switch (module.smooth.getValue()) {
            case NONE -> null;
            case RUNNING_AVERAGER -> new RunningAverager(module.smoothInterval.getIntValue());
            case AVERAGER -> new Averager(module.smoothInterval.getIntValue());
         };
         if (smoother != null) {
            noiseMasks.get(channel).add(smoother);
         }

         BaseNoiseMask masker = switch (module.mask.getValue()) {
            case NONE -> null;
            case DYNAMIC -> new DynamicNoiseMask(khz);
         };
         if (masker != null) {
            noiseMasks.get(channel).add(masker);
         }

         if (module.minimumQuality.getFloatValue() > 0) {
            noiseMasks.get(channel).add(new QualityMask(module.minimumQuality.getFloatValue()));
         }

         if (module.thresholdMasking.getBooleanValue()) {
            noiseMasks.get(channel).add(new ThresholdMask(module.threshold.getFloatValue()));
         }

         for (BaseNoiseMask mask : noiseMasks.get(channel)) {
            mask.setLogLabel(khz + " khz: ");
         }

         double lowLimit = 0;
         BaseHistogram histogram = switch (module.histogram.getValue()) {
            case GEOMETRIC -> new GeometricHistogram(lowLimit, 1.01);
            case LINEAR -> new LinearHistogram(lowLimit);
            case CONSTANT -> new SimpleHistogram(lowLimit);
         };

         histogram.setLogLabel(khz + " khz: ");

         histogram.setInitializationCellCount(module.histogramInitializationCellCount.getIntValue());
         histogram.setMaximumCellCount(module.histogramMaximumCellCount.getIntValue());
         histogram.setInitializationSampleCount(module.histogramInitializationSampleCount.getIntValue());
         histogram.setMinimumSampleCount(module.histogramMinimumSampleCount.getIntValue());
         histogram.setSmooth(module.histogramSmooth.getBooleanValue());
         histogram.setSmoothFactor(module.histogramSmoothFactor.getFloatValue());

         histograms[channel] = histogram;
      }

      selectedFallbackNoiseChannels = Set.copyOf(module.fallbackNoiseChannels.selectedChannels(rawFileConfiguration));
   }

   private NoiseFile createNoiseFile(RawFileConfiguration rawFileConfiguration) {
      Path file;
      Path koronaDirectory = getComputationContext().getAssociatedKoronaDirectory();
      if (koronaDirectory != null) {
         file = koronaDirectory.resolve(NoiseQuantificationModule.NOISE_XML);
         Log.global.info("Writing noise file to " + file);
      } else {
         file = null;
         Log.global.info("Not saving noise file (no output directory). ");
      }

      return new NoiseFile(rawFileConfiguration, file, module.getMaxIntervalsInNoiseFile());
   }

   BaseHistogram getHistogram(int channel) {
      return histograms[channel];
   }

   boolean getSdevMasking() {
      return module.sdevMasking.getBooleanValue();
   }

   float getSdev() {
      return module.sdev.getFloatValue();
   }

   boolean getSNMasking() {
      return module.snMasking.getBooleanValue();
   }

   float getSNRatio() {
      return module.snRatio.getFloatValue();
   }

   private void histogramInput(Ping ping) {
      ping.getNonNullPowerDatas().forEach(powerData -> {
         int channel = powerData.getChannel();
         HistogramData histogramData = new HistogramData(powerData);
         RangeValues rangeValues = rangeFunction.apply(ping, channel);
         for (BaseNoiseMask mask : noiseMasks.get(channel)) {
            histogramData = mask.mask(histogramData, ping, rangeValues);
         }
         histograms[channel].input(histogramData);
         if (module.writePlotParameters.getBooleanValue()) {
            PlotParameterUtils.getOrCreateValueSubDatagram(ping)
                  .putPerChannel(NOISE_DATA_CATEGORY, channel, histogramData != null ? histogramData.getQuality().category : -1);
         }
      });
   }

   private void createNQPDatagram(short channel, Ping ping) {
      NoiseQuantificationModule.NQP nqp = NoiseQuantificationModule.NQP.create(histograms[channel],
            module.sdevMasking.getBooleanValue(), module.sdev.getFloatValue(),
            module.snMasking.getBooleanValue(), module.snRatio.getFloatValue());

      NoiseFile.NoiseData noiseData;
      if (nqp != null) {
         noiseData = toNoiseData(nqp, channel);
         noiseFile.update(noiseData, channel, ping.getInstant());
      } else {
         noiseData = null;
      }

      if (module.useFallbackNoiseQuantile.getBooleanValue() && selectedFallbackNoiseChannels.contains((int) channel)) {
         NoiseFile.NoiseData quantileNoiseData = noiseFile.findQuantileNoiseData(channel, module.fallbackNoiseQuantile.getFloatValue() / 100);
         if (quantileNoiseData != null) {
            ping.add(new Nqp0Datagram(ping.getInstant(), channel, quantileNoiseData.ne(), quantileNoiseData.nh(), quantileNoiseData.quality()));
         }
      } else {
         if (noiseData != null) {
            ping.add(new Nqp0Datagram(ping.getInstant(), channel, noiseData.ne(), noiseData.nh(), noiseData.quality()));
         } else {
            noiseFile.addFallbackNqp0Datagram(channel, ping);
         }
      }

      if (module.writePlotParameters.getBooleanValue()) {
         PlotParameterUtils.getOrCreateValueSubDatagram(ping)
               .putPerChannel(NOISE_HISTOGRAM_CELL_COUNT, channel, histograms[channel].getCellCount())
               .putPerChannel(NOISE_HISTOGRAM_SAMPLE_COUNT, channel, histograms[channel].getSampleCount());
      }
   }

   private NoiseFile.NoiseData toNoiseData(NoiseQuantificationModule.NQP nqp, short channel) {
      float nh = getUserLimitedNh(nqp);

      // If Noise histogram looks odd, it is likely because the histogram does not really represent noise:
      // above a certain ration: the larger probNH/probNE is, the less likely the histogram is to represent noise.
      float qualityCorrect = 1.0f; //RJK: used only for plotting. Keep or remove?
      if (nqp.getNT() > 0 && nqp.getNE() > 0) {
         double r = Math.min(nqp.getNT() / nqp.getNE(), nqp.getNE() / nqp.getNT());
         if (r < 0.33) { //&& histograms[channel].getOverallQuality() >= 90) {
            qualityCorrect = (float) (0.67 + r);
         }
      }
      float overallQuality = histograms[channel].getOverallQuality() * qualityCorrect;

      return new NoiseFile.NoiseData(nqp.getNE(), nh, overallQuality);
   }

   private float getUserLimitedNh(NoiseQuantificationModule.NQP nqp) {
      float nh = nqp.getNH();       // Original high noise. Still used to calculate NE until 2023.12.12 - from then NHH is used
      if (getSdevMasking()) {       // Specifies user defined limit for high noise.
         nh = nqp.getNHH();         // New high noise specified by user. Default nh = getNH().
      }
      return nh;
   }

   private void produceNQPWithBuffer(int allowedBufferSize) {
      for (short channel = 1; channel < histograms.length; channel++) {
         if (!histograms[channel].isOK() && timeStepBuffer.size() < allowedBufferSize) {
            continue;
         }
         while (true) {
            int index = timeStepBufferIndices[channel];

            if (index == timeStepBuffer.size()) {
               break;
            }

            Ping timeStep = timeStepBuffer.get(index);

            if (!module.centerTimeStepBuffer.getBooleanValue() || histograms[channel].getCenterTime().isAfter(timeStep.getInstant())
                  || timeStepBuffer.size() - index > allowedBufferSize) {
               // Add reading from file here if histograms is not OK.
               createNQPDatagram(channel, timeStep);
               timeStepBufferIndices[channel]++;
               continue;
            }

            break;
         }
      }
   }

   private void produceNQPWithoutBuffer(Ping ping) {
      int transducerCount = ping.getRawFileConfiguration().getTransducerCount();
      for (short channel = 1; channel <= transducerCount; channel++) {
         createNQPDatagram(channel, ping);
      }
   }

   private void decreaseTimeStepBuffer() {
      int minIndex = timeStepBuffer.size();
      for (int channel = 1; channel < timeStepBufferIndices.length; channel++) {
         minIndex = Math.min(minIndex, timeStepBufferIndices[channel]);
      }
      if (timeStepBuffer.size() > module.timeStepBufferMaxSize.getIntValue()) {
         //Log.global.fine("timeStepBuffer.size() > timeStepBufferMaxSize "
         //      + timeStepBuffer.size() + " " + timeStepBufferMaxSize);
      }
      for (int i = 0; i < minIndex; i++) {
         outputQueue.add(timeStepBuffer.removeFirst());
      }
      for (int channel = 1; channel < timeStepBufferIndices.length; channel++) {
         timeStepBufferIndices[channel] -= minIndex;
      }
   }

   @Override
   protected @Nullable Ping generateOutput() throws IOException {
      while (outputQueue.isEmpty()) {
         if (getAsyncHandle().isCancelled()) {
            return null;
         }

         Ping ping = inputPing();

         if (ping == null) {
            if (module.timeStepBufferMaxSize.getIntValue() > 0) {
               produceNQPWithBuffer(0);
               decreaseTimeStepBuffer();
            }
            break;
         }

         ping.removeAll(Nqp0Datagram.class);

         if (module.timeStepBufferMaxSize.getIntValue() > 0) {
            timeStepBuffer.add(ping);
            histogramInput(ping);
            produceNQPWithBuffer(module.timeStepBufferMaxSize.getIntValue());
            decreaseTimeStepBuffer();
         } else {
            histogramInput(ping);
            produceNQPWithoutBuffer(ping);
            outputQueue.add(ping);
         }
      }
      return outputQueue.poll();
   }
}
