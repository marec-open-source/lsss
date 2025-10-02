package no.imr.korona.computation.noise;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.datagrams.Nqp0Datagram;
import no.imr.korona.data.formats.synthetic.SyntheticDataFormatPlugin;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.util.TvgArray;
import no.imr.tools.math.Median;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.TreeMap;

final class NoiseMedianQuantificationModuleComputation extends GeneralPingModuleComputation {
   private final NoiseMedianQuantificationModule module;
   private final Queue<Ping> outputQueue = new ArrayDeque<>();
   private final List<PerChannelNoiseFile> medianNoiseData = new ArrayList<>();
   private final Map<Integer, PerChannelNoiseData> runningMedianNoiseData = new TreeMap<>();
   private boolean firstTime = true;
   private boolean hasWrittenNoiseData;

   NoiseMedianQuantificationModuleComputation(NoiseMedianQuantificationModule module, ComputationContext computationContext, PingSource pingSource) {
      super(module, computationContext, pingSource);

      this.module = module;
   }

   private void detectNoiseFromStart() throws IOException {
      List<Ping> buffer = new ArrayList<>();
      while (buffer.size() < module.pingHistory.getIntValue()) {
         Ping ping = inputPing();
         if (ping == null) {
            break;
         }
         buffer.add(ping);
         detectNoise(ping);
      }
      for (Ping ping : buffer) {
         processPing(ping);
         outputQueue.add(ping);
      }
   }

   private void detectNoise(Ping ping) {
      ping.getNonNullPowerDatas().forEach(powerData -> {
         PerChannelNoiseData data = runningMedianNoiseData.get(powerData.getChannel());
         if (data == null) { // If never detected, or this is a new detection.
            data = new PerChannelNoiseData(module.noiseSamplesPerBeam.getIntValue(), module.pingHistory.getIntValue(),
                  module.detectionDistance.getFloatValue(), module.innermostDistance.getBooleanValue());
            runningMedianNoiseData.put(powerData.getChannel(), data);
         }
         data.update(powerData);
      });
   }

   @Override
   protected @Nullable Ping generateOutput() throws IOException {
      if (firstTime) {
         firstTime = false;
         detectNoiseFromStart();
      }
      if (!outputQueue.isEmpty()) {
         return outputQueue.remove();
      }
      Ping ping = inputPing();
      if (ping != null) {
         processPing(ping);
      } else {
         if (!hasWrittenNoiseData) {
            writeNoiseFiles();
            hasWrittenNoiseData = true;
         }
      }
      return ping;
   }

   private void processPing(Ping ping) {
      ping.removeAll(Nqp0Datagram.class);

      detectNoise(ping);
      NavigableMap<Integer, PerChannelNoiseFile.NoiseData> noiseData = new TreeMap<>();
      for (Map.Entry<Integer, PerChannelNoiseData> integerPerChannelNoiseDataEntry : runningMedianNoiseData.entrySet()) {
         integerPerChannelNoiseDataEntry.getValue().updateMedianNoise();
         float ne = integerPerChannelNoiseDataEntry.getValue().getCurrentNoiseValue();
         noiseData.put(integerPerChannelNoiseDataEntry.getKey(), new PerChannelNoiseFile.NoiseData(ne, ne * 2));
      }
      medianNoiseData.add(new PerChannelNoiseFile(null, noiseData));

      // Send nqp datagrams.
      ping.getNonNullPowerDatas().forEach(powerData -> {
         int channel = powerData.getChannel();
         PerChannelNoiseData data = runningMedianNoiseData.get(channel);
         float averageNoise;
         float upperLimitNoise;
         if (data != null) {
            averageNoise = data.getCurrentNoiseValue();
            // Defining upper limit at 2 times average noise value.
            upperLimitNoise = 2 * averageNoise;
         } else {
            averageNoise = 0;
            upperLimitNoise = 0;
         }
         Nqp0Datagram noiseDatagram = new Nqp0Datagram(powerData.getNTDate(), (short) channel,
               averageNoise, upperLimitNoise, 100);
         ping.add(noiseDatagram);
      });
   }

   private void writeNoiseFiles() throws IOException {
      Path inputFile = getComputationContext().getPingReader().getFile();
      if (inputFile.toString().endsWith(SyntheticDataFormatPlugin.LSSS_SS_SUFFIX)) { // No noise file for synthetic data.
         return;
      }
      Path koronaDirectory = getComputationContext().getAssociatedKoronaDirectory();
      if (koronaDirectory == null) {
         return;
      }
      Path noiseDir = koronaDirectory.resolve(NoiseUtils.MEDIAN_NOISE_SUBFOLDER);
      RawFileConfiguration rawFileConfiguration = getPingConfiguration().getRawFileConfiguration();

      new PerChannelNoiseFile(NoiseUtils.getPerFileNoiseFile(noiseDir, rawFileConfiguration), NoiseUtils.medianNoiseData(medianNoiseData)).write();

      List<PerChannelNoiseFile> dayFiles = NoiseUtils.parseNoiseFiles(noiseDir.resolve(NoiseUtils.FILE_NOISE_FILE_PREFIX), NoiseUtils.dayFilePredicate(rawFileConfiguration));
      new PerChannelNoiseFile(NoiseUtils.getDayNoiseFile(noiseDir, rawFileConfiguration, NoiseUtils.DAY_NOISE_DIR), NoiseUtils.medianNoiseData(dayFiles)).write();
      new PerChannelNoiseFile(NoiseUtils.getDayNoiseFile(noiseDir, rawFileConfiguration, NoiseUtils.DAY_NOISE_LOW_DIR), NoiseUtils.lowNoiseData(dayFiles)).write();
      new PerChannelNoiseFile(NoiseUtils.getDayNoiseFile(noiseDir, rawFileConfiguration, NoiseUtils.DAY_NOISE_HIGH_DIR), NoiseUtils.highNoiseData(dayFiles)).write();

      new PerChannelNoiseFile(NoiseUtils.getSurveyNoiseFile(noiseDir), NoiseUtils.medianNoiseData(NoiseUtils.parseNoiseFiles(noiseDir.resolve(NoiseUtils.DAY_NOISE_DIR), NoiseUtils.dayDirFilePredicate()))).write();
      new PerChannelNoiseFile(NoiseUtils.getSurveyLowNoiseFile(noiseDir), NoiseUtils.lowNoiseData(NoiseUtils.parseNoiseFiles(noiseDir.resolve(NoiseUtils.DAY_NOISE_LOW_DIR), NoiseUtils.dayDirFilePredicate()))).write();
      new PerChannelNoiseFile(NoiseUtils.getSurveyHighNoiseFile(noiseDir), NoiseUtils.highNoiseData(NoiseUtils.parseNoiseFiles(noiseDir.resolve(NoiseUtils.DAY_NOISE_HIGH_DIR), NoiseUtils.dayDirFilePredicate()))).write();
   }

   private static final class PerChannelNoiseData {
      private final Deque<Float> medianHistory = new ArrayDeque<>();
      private final int localNoiseSamplePerBeam;
      private final float detectionDistance;
      private final boolean innermostDistance;
      private final int pingHistoryLength;
      private float currentNoiseValue;

      private static final float UNDEF_VALUE = Float.NaN;

      private PerChannelNoiseData(int localNoiseSamplePerBeam, int pingHistoryLength, float detectionDistance, boolean innermostDistance) {
         this.localNoiseSamplePerBeam = localNoiseSamplePerBeam;
         this.pingHistoryLength = pingHistoryLength;
         this.detectionDistance = detectionDistance;
         this.innermostDistance = innermostDistance;
      }

      private void update(PowerData powerData) {
         // Extract samples from PowerData.
         // Use only samples at a minimal range of 'max range' - detectionDistance from the transducer.
         int beginIndex;
         int endIndex;
         if (innermostDistance) {
            beginIndex = 0;
            endIndex = Math.clamp((int) Math.floor(powerData.rangeToSampleIndexAsFloat(powerData.getMinRange() + detectionDistance)), 0, powerData.getCount());
         } else {
            beginIndex = Math.clamp((int) Math.ceil(powerData.rangeToSampleIndexAsFloat(powerData.getMaxRange() - detectionDistance)), 0, powerData.getCount());
            endIndex = powerData.getCount();
         }
         if (beginIndex > endIndex) {
            updateMedianHistory(UNDEF_VALUE);
            return;
         }

         int samplingInterval = (int) Math.ceil((double) (endIndex - beginIndex) / localNoiseSamplePerBeam);
         int actualNoiseSamples = (int) Math.ceil((double) (endIndex - beginIndex) / samplingInterval);
         if (actualNoiseSamples == 0) {
            updateMedianHistory(UNDEF_VALUE);
            return;
         }
         float[] noiseValues = new float[actualNoiseSamples];
         float[] sv = powerData.getSv();
         TvgArray tvg = powerData.getTVGArray();
         for (int i = beginIndex, index = 0; index < actualNoiseSamples; i += samplingInterval, index++) {
            noiseValues[index] = sv[i] / tvg.get(i);
         }

         //find median
         float medianValue = Median.quickSelect(noiseValues);
         updateMedianHistory(medianValue);
      }

      private void updateMedianHistory(float medianValue) {
         medianHistory.add(medianValue);
         if (medianHistory.size() > pingHistoryLength) {
            medianHistory.removeFirst();
         }
      }

      private void updateMedianNoise() {
         // Update current noise value find median in deque.
         float[] allMedians = new float[medianHistory.size()];
         int index = 0;
         for (Float value : medianHistory) {
            if (!value.equals(UNDEF_VALUE)) {
               allMedians[index] = value;
               index++;
            }
         }
         if (index > 0) {
            currentNoiseValue = Median.quickSelect(allMedians, 0, index);
         } else {
            currentNoiseValue = UNDEF_VALUE;
         }
      }

      private float getCurrentNoiseValue() {
         return currentNoiseValue;
      }
   }
}
