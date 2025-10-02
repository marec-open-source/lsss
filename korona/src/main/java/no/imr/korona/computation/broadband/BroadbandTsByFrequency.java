package no.imr.korona.computation.broadband;

import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.math.ComplexArrayUtils;
import no.imr.tools.math.FftCache;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.IntRange;
import org.jtransforms.fft.DoubleFFT_1D;

import java.util.function.IntToDoubleFunction;

/**
 * Computing TS from broadband data.
 */
public final class BroadbandTsByFrequency extends BroadbandByFrequency {
   public BroadbandTsByFrequency(BroadbandData broadbandData) {
      super(broadbandData);
   }

   public Result calculate(FloatRange depthRange, FloatRange frequencyRange) {
      int beginIndex = broadbandData.depthToClampedSampleIndex(depthRange.min());
      int endIndex = broadbandData.depthToClampedSampleIndex(depthRange.max());
      int nw = endIndex - beginIndex;
      int fftLength = calculateRequiredFftLength(nw);

      ComplexArray averagedPulseCompressedSignal = averagePulseCompressedSignal.copyOfRange(beginIndex, endIndex, fftLength);

      boolean useTVG = getUseTVG();

      int maxIndex = beginIndex + ComplexArrayUtils.maxIndex(averagedPulseCompressedSignal, 0, nw); // Target index for depth correction and signal reduction, absolute

      ComplexArray reducedAutoCorrelatedSignal = getReducedAutoCorrelatedSignal(
            beginIndex - maxIndex, endIndex - maxIndex, fftLength);

      DoubleFFT_1D fft = FftCache.getDouble1D(fftLength);
      fft.complexForward(averagedPulseCompressedSignal.values());

      int shift = (int) (shiftFactor * fftLength);

      IntRange indexRange = frequencyRangeToIndexRange(frequencyRange, fftLength);

      float[] ts = new float[indexRange.getSize()];

      // not frequency-dependent term
      double r = broadbandData.getTvgRange(maxIndex);
      double constantTerm = (useTVG ? 40 * Math.log10(r) : 0)
            - 10 * Math.log10(broadbandData.getTransmitPower() / (16 * Math.PI * Math.PI));

      IntToDoubleFunction frequencies = getFrequencies(fftLength);

      for (int i = indexRange.begin(); i < indexRange.end(); i++) {
         int iShifted = (i + shift) % fftLength;

         // Divide by FFT of transmitted signal and compute absolute value squared.
         // |(a + ib)/(c + id)|^2 = |a + ib|^2 / |c + id|^2
         double prx = averagedPulseCompressedSignal.abs2(iShifted) / reducedAutoCorrelatedSignal.abs2(iShifted);
         double f = frequencies.applyAsDouble(i);
         double fNonZero = f > 0 ? f : frequencies.applyAsDouble(1); // f for division, avoids f=0
         double lambda = broadbandData.getSoundVelocity() / fNonZero;

         ts[i - indexRange.begin()] = (float) (10 * Math.log10(prx)
               + (useTVG ? 2 * r * absorption.getAbsorption(f) : 0)
               - 10 * Math.log10(lambda * lambda)
               - 2 * broadbandData.getGain(fNonZero)
               + broadbandData.getDirectivityCorrection(maxIndex, (float) fNonZero)
               + 10 * Math.log10(broadbandData.getPrxFactor(fNonZero))
               + constantTerm);
      }

      return new Result(ts, maxIndex);
   }

   public record Result(float[] values, int sampleIndex) {
   }
}
