package no.imr.korona.computation.broadband;

import com.google.common.math.IntMath;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.util.absorption.Absorption;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.math.ComplexArrayUtils;
import no.imr.tools.math.FftCache;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.IntRange;

import java.util.function.IntToDoubleFunction;

/**
 * Base class for converting broadband data.
 */
public abstract class BroadbandByFrequency {
   final BroadbandData broadbandData;
   final Absorption absorption;
   private final PulseCompression pulseCompression;
   final ComplexArray averagePulseCompressedSignal;
   private final int minimalRequiredFftLength;
   final float shiftFactor; // Using periodicity of DFT
   private final ComplexArray autoCorrelationTransmitSignal;
   private final int peakIndex;
   private final float minFrequency;
   private final float fsDec;
   private boolean useTVG = true;

   BroadbandByFrequency(BroadbandData broadbandData) {
      this.broadbandData = broadbandData;
      absorption = broadbandData.getAbsorption();
      pulseCompression = broadbandData.getPulseCompression();
      averagePulseCompressedSignal = broadbandData.getAveragePulseCompressedSignal();
      autoCorrelationTransmitSignal = broadbandData.getPulseCompression().getAutoCorrelationTransmitSignal();
      peakIndex = ComplexArrayUtils.maxIndex(autoCorrelationTransmitSignal);
      minimalRequiredFftLength = 8 * IntMath.ceilingPowerOfTwo(autoCorrelationTransmitSignal.length());

      fsDec = 1 / broadbandData.getSampleInterval();
      minFrequency = pulseCompression.getConfig().frequencyRange().min();
      shiftFactor = minFrequency / fsDec;
   }

   boolean getUseTVG() {
      return useTVG;
   }

   public void setUseTVG(boolean useTVG) {
      this.useTVG = useTVG;
   }

   int calculateRequiredFftLength(int signalLength) {
      return Math.max(IntMath.ceilingPowerOfTwo(signalLength), minimalRequiredFftLength);
   }

   public FloatRange getMaxFrequencyRange() {
      return FloatRange.ofMinAndSize(minFrequency, fsDec);
   }

   IntToDoubleFunction getFrequencies(int frequencyCount) {
      double df = fsDec / (frequencyCount - 1);
      return i -> minFrequency + i * df;
   }

   IntRange frequencyRangeToIndexRange(FloatRange frequencyRange, int frequencyCount) {
      float df = fsDec / (frequencyCount - 1);
      int iBegin = Math.clamp(Math.round((frequencyRange.min() - minFrequency) / df), 0, frequencyCount);
      int iEnd = Math.clamp(Math.round((frequencyRange.max() - minFrequency) / df) + 1, 0, frequencyCount);
      return new IntRange(iBegin, iEnd);
   }

   public int getConvolutionKernelSize() {
      return pulseCompression.getConvolutionKernelSize();
   }

   ComplexArray getReducedAutoCorrelatedSignal(int beginIndexRelPeak, int endIndexRelPeak, int fftLength) {
      /*
       * trim auto-correlated signal to part of target signal we are looking at,
       * apply padding and transform to frequency domain
       */

      int beginIndex = Math.max(peakIndex + beginIndexRelPeak, 0);
      int endIndex = Math.min(peakIndex + endIndexRelPeak + 1, autoCorrelationTransmitSignal.length());
      ComplexArray reducedAutoCorrelatedSignal = autoCorrelationTransmitSignal.copyOfRange(beginIndex, endIndex, fftLength);
      FftCache.getDouble1D(fftLength).complexForward(reducedAutoCorrelatedSignal.values());
      return reducedAutoCorrelatedSignal;
   }
}
