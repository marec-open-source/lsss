package no.imr.korona.util.ts;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.NarrowbandData;
import no.imr.tools.math.ComplexArrayUtils;
import no.imr.tools.math.PeakFinding;

import java.util.List;

public final class PeakTSDetector implements TSDetector {
   private final float minTs;
   private final float maxGainCompensation;
   private final float pulseLengthDeterminationLevel;
   private final float minEchoLength;
   private final float maxEchoLength;
   private final boolean doPhaseDeviationCheck;
   private final float maxPhaseDevPhaseSteps;
   private final float maxDepth;

   public PeakTSDetector(float minTs, float maxGainCompensation, float pulseLengthDeterminationLevel,
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
      return getTsDetections(channelData, Math.min(beginIndex, maxIndex), Math.min(endIndex, maxIndex));
   }

   private boolean isAccepted(ChannelData channelData, TSDetection tsDetection) {
      if (!TSDetectorUtils.checkEchoLimits(tsDetection, channelData, minEchoLength, maxEchoLength)) {
         return false;
      }
      AngleData angleData = channelData.getAngleData();
      if (!TSDetectorUtils.checkPhaseSamplesPhaseSteps(tsDetection, angleData, doPhaseDeviationCheck, maxPhaseDevPhaseSteps)) {
         return false;
      }
      float oneWayGainCompensation = TSDetectorUtils.computeOneWayGainCompensation(tsDetection, angleData, channelData.getTransducer());
      if (oneWayGainCompensation > maxGainCompensation) {
         return false;
      }
      if (channelData.getTSU(tsDetection.peakIndex()) + 2 * oneWayGainCompensation < minTs) {
         return false;
      }
      return true;
   }

   private List<TSDetection> getTsDetections(ChannelData channelData, int beginIndex, int endIndex) {
      // 20log(signal_peak) - 20log(signal_otherSample) < pulseLengthDeterminationLevel
      // --> signal_peak/signal_otherSample < 10^(pulseLengthDeterminationLevel/20)
      // signal_peak * 10^(-pulseLengthDeterminationLevel/20) < signal_otherSample
      float linearPulseLengthDeterminationLevel = (float) Math.pow(10, -pulseLengthDeterminationLevel / 20);
      float[] values;
      int maxScale;
      switch (channelData) {
         case BroadbandData broadbandData -> {
            values = broadbandData.getAveragePulseCompressedSignal().abs2AsFloats();
            maxScale = broadbandData.getPulseCompression().getConvolutionKernelSize();
         }
         case NarrowbandData narrowbandData -> {
            values = ComplexArrayUtils.matchFilterArray(narrowbandData.computeAverageComplexValues(), narrowbandData.getSentSignal()).abs2AsFloats();
            maxScale = (int) (maxEchoLength * channelData.getPulseDuration() / channelData.getSampleInterval());
         }
         default -> {
            values = channelData.getPowerData().computeNoisePowerIndex();
            maxScale = (int) (maxEchoLength * channelData.getPulseDuration() / channelData.getSampleInterval());
         }
      }
      List<PeakFinding.Peak> peaks = PeakFinding.getPeaks(values, maxScale, linearPulseLengthDeterminationLevel);
      return peaks.stream()
            .filter(peak -> peak.peakIndex() >= beginIndex && peak.peakIndex() < endIndex)
            .map(peak -> new TSDetection(peak.beginIndex(), peak.peakIndex(), peak.endIndex()))
            .filter(tsDetection -> isAccepted(channelData, tsDetection))
            .toList();
   }
}
