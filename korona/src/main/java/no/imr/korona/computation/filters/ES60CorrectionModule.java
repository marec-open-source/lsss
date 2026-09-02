package no.imr.korona.computation.filters;

import no.imr.korona.computation.BaseBufferedPingModule;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.math.Mean;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ES60CorrectionModule extends BaseBufferedPingModule {
   private static final int WAVE = 2721;
   private static final int TURN1 = WAVE / 4 + 1;
   private static final int TURN2 = WAVE * 3 / 4 + 1;
   private static final int EPSILON = 50;
   private static final int START_SAMPLE = 0;
   private static final int END_SAMPLE = 4;
   static final int NUM_SAMPLES = END_SAMPLE - START_SAMPLE + 1;  //number of samples to use in summation

   public final IntParameter reliabilityDetectionWindow = new IntParameter(
         new Name("ReliabilityDetectionWindow", "Reliability detection window"),
         5, Unit.COUNT,
         "Window used when computing statistics used when determining best fit triangle");

   public final BooleanParameter performScan = new BooleanParameter(
         new Name("PerformScan", "Perform scan"),
         false,
         "Will perform a scan for the triangle index of the first ping in the next file");

   public final BooleanParameter continueAcrossFiles = new BooleanParameter(
         new Name("ContinueAcrossFiles", "Continue across files"),
         false,
         "Continue the current wave index number across files");

   private @Nullable ES60CorrectionModuleJob job;

   public ES60CorrectionModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            reliabilityDetectionWindow,
            performScan,
            continueAcrossFiles
      );
   }

   @Override
   public GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      if (job == null || !continueAcrossFiles.getBooleanValue()) {
         job = new ES60CorrectionModuleJob(this, pingSource);
      }
      return new ES60CorrectionModuleComputation(this, computationContext, pingSource, job);
   }

   public static final class ES60CorrectionModuleComputation extends BaseBufferedPingModuleComputation {
      private final ES60CorrectionModuleJob job;

      private ES60CorrectionModuleComputation(ES60CorrectionModule module, ComputationContext computationContext, PingSource pingSource, ES60CorrectionModuleJob job) {
         super(module, computationContext, pingSource);

         this.job = job;
         if (job.transientPerformScan) {
            setPingsToBuffer(WAVE);
         } else {
            setPingsToBuffer(0);
         }
      }

      public ES60CorrectionModuleJob getJob() {
         return job;
      }

      @Override
      protected @Nullable Ping generateOutput() throws IOException {
         return job.generateOutput(this);
      }
   }

   public static final class ES60CorrectionModuleJob {
      private final ES60CorrectionModule module;
      private final List<ChannelStatistics> channelStatistics = new ArrayList<>();
      private int pingCounter = 0;
      private boolean transientPerformScan;

      private ES60CorrectionModuleJob(ES60CorrectionModule module, PingSource pingSource) {
         this.module = module;
         transientPerformScan = module.performScan.getBooleanValue();
         PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
         RawFileConfiguration rawFileConfiguration = pingConfiguration.getRawFileConfiguration();
         for (int channel = 1; channel <= rawFileConfiguration.getTransducerCount(); channel++) {
            channelStatistics.add(new ChannelStatistics(channel));
         }
      }

      public boolean isFirstTriangleIndexReliable() {
         return allFirstIndicesReliable();
      }

      public int getPingsUsed() {
         int answer = 0;
         for (ChannelStatistics channelStatistic : channelStatistics) {
            if (channelStatistic.means != null) {
               answer = Math.max(answer, (int) channelStatistic.means.getCount());
            }
         }
         return answer;
      }

      static short waveAdjustment(int triangleIndexNo) {
         int adj = triangleIndexNo % WAVE;
         if (adj > WAVE * 3 / 4) {
            adj -= WAVE;
         } else if (adj > WAVE / 4) {
            adj = WAVE / 2 - adj;
         }
         adj /= 16;
         return (short) adj;
      }

      private void adjustPing(int triangleIndexNo, Ping ping) {
         for (ChannelStatistics channelStatistic : channelStatistics) {
            channelStatistic.adjustPing(triangleIndexNo, ping);
         }
      }

      private static int getSum(int start, int end, PowerData powerData) {
         if (powerData.getCount() <= end) {
            throw new ArrayIndexOutOfBoundsException("Not enough samples in datagram");
         }
         short[] shortPower = powerData.computeShortPower();
         int sum = 0;
         for (int i = start; i <= end; i++) {
            sum += shortPower[i];
         }
         return sum;
      }

      private void updateDeviations(Ping ping) {
         for (ChannelStatistics channelStatistic : channelStatistics) {
            channelStatistic.updateDeviations(ping);
         }
      }

      private boolean allFirstIndicesReliable() {
         for (ChannelStatistics channelStatistic : channelStatistics) {
            if (!channelStatistic.isFirstTriangleIndexReliable()) {
               return false;
            }
         }
         return true;
      }

      private @Nullable Ping generateOutput(ES60CorrectionModuleComputation computation) throws IOException {
         Ping next = computation.pollFirstPingInBuffer();
         if (next == null) {
            return null;
         }

         if (transientPerformScan) {
            updateDeviations(next);
            pingCounter++;
            for (Ping ping : computation.getPingOutputQueue()) {
               //update with all pings in buffer
               updateDeviations(ping);
               pingCounter++;
               pingCounter %= WAVE;
            }
            pingCounter = 0;
            computation.setPingsToBuffer(0);
            transientPerformScan = false;
         }

         // if the index of first ping is unreliable, we do an update of the statistics for each ping
         if (!allFirstIndicesReliable()) {
            updateDeviations(next);
         }
         if (allFirstIndicesReliable()) {
            adjustPing(pingCounter, next);
         }
         pingCounter++;
         pingCounter %= WAVE;

         return next;
      }

      private final class ChannelStatistics {
         private boolean firstTriangleIndexReliable = false;

         private DeviationHistogram @Nullable [] deviations = new DeviationHistogram[WAVE];
         private Mean @Nullable [] waveMeans = new Mean[WAVE];
         private @Nullable Mean means = new Mean();

         private int firstTriangleWaveIndex = 0;

         private final int channel;

         private ChannelStatistics(int channel) {
            this.channel = channel;
            for (int i = 0; i < WAVE; i++) {
               waveMeans[i] = new Mean();
               deviations[i] = new DeviationHistogram(i);
            }
         }

         private void updateDeviations(Ping ping) {
            if (firstTriangleIndexReliable || means == null || waveMeans == null || deviations == null) {
               return;
            }
            PowerData powerData = ping.getPowerData(channel);
            if (powerData == null) {
               return;
            }
            if (powerData.getCount() <= END_SAMPLE) {
               return;
            }
            double value = getSum(START_SAMPLE, END_SAMPLE, powerData);
            means.update(value);
            double mean = means.getMean(); //running average
            int waveNo = 0;
            for (Mean waveMean : waveMeans) {
               waveMean.update(waveAdjustment(waveNo + pingCounter));
               deviations[waveNo].updateHist(pingCounter, value, mean, NUM_SAMPLES * waveMean.getMean());
               waveNo++;
            }
            int bestFitIndex = 0; //redetection each ping
            int index = 0;
            double smallestDevSum = Double.MAX_VALUE;
            int window = module.reliabilityDetectionWindow.getIntValue();
            for (DeviationHistogram deviation : deviations) {
               double devSum = deviation.getSum();
               double nVals = 1;
               for (int i = index - window; i <= index + window; i++) {
                  int waveIndex = i < 0 ? (i + WAVE) % WAVE : i % WAVE;
                  double weight = i < 0 ? 1.0 / (1 - i) : 1.0 / (1 + i);
                  nVals += weight;
                  devSum += weight * deviations[waveIndex].getSum();
               }
               devSum /= nVals;
               if (devSum < smallestDevSum) {
                  smallestDevSum = devSum;
                  bestFitIndex = index;
               }
               index++;
            }
            //test if the start index is very close to a turning point
            boolean toCloseToTurningPoint = (TURN1 - EPSILON < bestFitIndex && TURN1 + EPSILON > bestFitIndex) ||
                  (TURN2 - EPSILON < bestFitIndex && TURN2 + EPSILON > bestFitIndex);
            //sample additional pings if start index is close to a turning point
            int pingsRequired = toCloseToTurningPoint ? WAVE / 2 + 2 * EPSILON : WAVE / 2;
            firstTriangleIndexReliable = means.getCount() > pingsRequired;
            if (firstTriangleIndexReliable) {
               //free the used memory
               means = null;
               waveMeans = null;
               deviations = null;
            }
            firstTriangleWaveIndex = bestFitIndex;
         }

         private void adjustPing(int pingCounter, Ping ping) {
            int triangleIndex = firstTriangleWaveIndex + pingCounter;
            short adjustment = waveAdjustment(triangleIndex);

            PowerData powerData = ping.getPowerData(channel);
            if (powerData != null) {
               short[] power = powerData.computeShortPower();
               for (int j = 0; j < power.length; j++) {
                  power[j] -= adjustment;
               }
               powerData.setSvFromShortPower(power);
            }
         }

         private boolean isFirstTriangleIndexReliable() {
            return firstTriangleIndexReliable;
         }
      }

      public static final class DeviationHistogram {
         private final int waveNumber;
         private double sum = 0;

         public DeviationHistogram(int waveNumber) {
            this.waveNumber = waveNumber;
         }

         public void updateHist(int pingNumber, double value, double runningMean, double waveMean) {
            double dev = deviation(value, pingNumber, waveMean, runningMean);
            //sum += Math.abs(dev);
            sum += dev * dev;
         }

         private double deviation(double value, int pingNumber, double waveMean, double runningMean) {
            return value - NUM_SAMPLES * waveAdjustment(waveNumber + pingNumber) +
                  (waveMean - runningMean);
         }

         public double getSum() {
            return sum;
         }
      }
   }
}
