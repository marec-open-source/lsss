package no.imr.korona.util.ts;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SedTSDetector implements TSDetector {
   private final float minTs;
   private final float maxGainCompensation;
   private final float pulseLengthDeterminationLevel;
   private final float minEchoLength;
   private final float maxEchoLength;
   private final boolean doPhaseDeviationCheck;
   private final float maxPhaseDevPhaseSteps;
   private final float maxDepth;

   public SedTSDetector(float minTs, float maxGainCompensation, float pulseLengthDeterminationLevel,
                        float minEchoLength, float maxEchoLength,
                        boolean doPhaseDeviationCheck, float maxPhaseDevPhaseSteps,
                        float maxDepth) {
      this.minTs = minTs;
      this.maxGainCompensation = maxGainCompensation;
      this.pulseLengthDeterminationLevel = pulseLengthDeterminationLevel;
      this.minEchoLength = minEchoLength;
      this.maxEchoLength = maxEchoLength;
      this.doPhaseDeviationCheck = doPhaseDeviationCheck;
      this.maxPhaseDevPhaseSteps = maxPhaseDevPhaseSteps;
      this.maxDepth = maxDepth;
   }

   @Override
   public List<TSDetection> getAcceptedTsDetections(Ping ping, ChannelData channelData, int beginIndex, int endIndex) {
      int maxIndex = channelData.depthToClampedSampleIndex(maxDepth);
      return getTsDetections(channelData.getPowerData(), Math.min(beginIndex, maxIndex), Math.min(endIndex, maxIndex));
   }

   private boolean isAccepted(PowerData powerData, TSDetection tsDetection) {
      if (!TSDetectorUtils.checkEchoLimits(tsDetection, powerData, minEchoLength, maxEchoLength)) {
         return false;
      }
      AngleData angleData = powerData.getAngleData();
      if (!TSDetectorUtils.checkPhaseSamplesPhaseSteps(tsDetection, angleData, doPhaseDeviationCheck, maxPhaseDevPhaseSteps)) {
         return false;
      }
      float oneWayGainCompensation = TSDetectorUtils.computeOneWayGainCompensation(tsDetection, angleData, powerData.getTransducer());
      if (oneWayGainCompensation > maxGainCompensation) {
         return false;
      }
      if (powerData.getTSU(tsDetection.peakIndex()) + 2 * oneWayGainCompensation < minTs) {
         return false;
      }
      if (hasMultiplePeaks(powerData, tsDetection)) {
         return false;
      }
      return true;
   }

   private List<TSDetection> getTsDetections(PowerData powerData, int iBegin, int iEnd) {
      List<TSDetection> tsDetections = new ArrayList<>();
      int maxSamples = (int) (4 * powerData.getPulseDuration() / powerData.getSampleInterval());
      int iBeginOrig = iBegin;
      int iEndOrig = iEnd;
      iBegin = Math.max(iBegin - maxSamples, 0);
      iEnd = Math.min(iEnd + maxSamples, powerData.getCount());
      float peakTS = -Float.MAX_VALUE;
      int peakTSIndex = -1;
      int startTSIndex = -1;
      float[] tsArray = new float[iEnd - iBegin + 1];
      Arrays.fill(tsArray, -Float.MAX_VALUE);
      for (int iSv = iBegin; iSv < iEnd; iSv++) {
         float ts = powerData.getTSU(iSv);
         tsArray[iSv - iBegin] = ts;
         if (ts >= minTs - 2 * maxGainCompensation - pulseLengthDeterminationLevel) {
            if (startTSIndex < 0) {
               startTSIndex = iSv;
            }
            if (ts > peakTS) {
               peakTS = ts;
               peakTSIndex = iSv;
            }
         } else {
            if (peakTSIndex >= iBeginOrig && peakTSIndex < iEndOrig) {
               if (iSv - startTSIndex + 1 < maxSamples) {
                  TSDetection tsDetection = getTsDetection(tsArray, peakTSIndex, iBegin, iSv, pulseLengthDeterminationLevel);
                  if (isAccepted(powerData, tsDetection)) {
                     tsDetections.add(tsDetection);
                  }
               }
            }
            startTSIndex = -1;
            peakTSIndex = -1;
            peakTS = -Float.MAX_VALUE;
         }
      }
      return tsDetections;
   }

   private static TSDetection getTsDetection(float[] tsArray, int peakIndex, int offset, int maxIndex, float pulseLengthDeterminationLevel) {
      float peakTS = tsArray[peakIndex - offset];
      float thresholdTS = peakTS - pulseLengthDeterminationLevel;
      int beginIndex = peakIndex;
      while (beginIndex - offset > 0 && tsArray[beginIndex - offset - 1] > thresholdTS) {
         beginIndex--;
      }
      int endIndex = peakIndex + 1;
      while (endIndex <= maxIndex && tsArray[endIndex - offset] > thresholdTS) {
         endIndex++;
      }
      return new TSDetection(beginIndex, peakIndex, endIndex);
   }

   private static boolean hasMultiplePeaks(PowerData powerData, TSDetection tsDetection) {
      if (tsDetection.getSampleCount() <= 3) {
         return false;
      }
      int beginIndex = tsDetection.beginIndex();

      int numberOfPeaks = 0;
      float prev = powerData.getTSU(beginIndex);
      float mid = powerData.getTSU(beginIndex + 1);
      float next = powerData.getTSU(beginIndex + 2);
      if (prev > mid) {
         numberOfPeaks++;
      } else if (mid > prev && mid > next) {
         numberOfPeaks++;
      }
      for (int i = 3; i < tsDetection.getSampleCount(); i++) {
         prev = mid;
         mid = next;
         next = powerData.getTSU(beginIndex + i);
         if (mid > prev && mid > next) {
            numberOfPeaks++;
         }
      }
      if (next > mid) {
         numberOfPeaks++;
      }
      return numberOfPeaks > 1;
   }
}
