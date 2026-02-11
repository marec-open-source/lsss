package no.imr.korona.computation.broadband;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.math.FftCache;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.IntRange;
import org.jtransforms.fft.DoubleFFT_1D;

import java.util.List;
import java.util.function.IntToDoubleFunction;
import java.util.stream.IntStream;

/**
 * Computing S<sub>v</sub> from broadband data.
 */
public final class BroadbandSvByFrequency extends BroadbandByFrequency {
   private static final LoadingCache<Integer, double[]> NORMALIZED_HANNING_WINDOW_CACHE = CacheBuilder.newBuilder()
         .maximumSize(20)
         .build(CacheLoader.from(nw -> {
            double[] w = PulseCompression.symHanning(nw);
            double wNormalizer = Math.sqrt(ArrayMath.sqSum(w) / nw);
            ArrayMath.divide(w, wNormalizer);
            return w;
         }));
   private final LoadingCache<ReducedAutoCorrelatedSignalCacheKey, ComplexArray> reducedAutoCorrelatedSignalCache = CacheBuilder.newBuilder()
         .maximumSize(10)
         .build(CacheLoader.from(key -> {
            return getReducedAutoCorrelatedSignal(key.beginIndexRelPeak, key.endIndexRelPeak, key.fftLength);
         }));
   private final LoadingCache<FrequencyTermCacheKey, FrequencyTerm> frequencyTermCache = CacheBuilder.newBuilder()
         .maximumSize(10)
         .build(CacheLoader.from(key -> {
            return computeFrequencyTerm(key.beginIndex, key.endIndex, key.fftLength);
         }));

   public BroadbandSvByFrequency(BroadbandData broadbandData) {
      super(broadbandData);
   }

   public List<FloatRange> windowDepthRanges(FloatRange depthRange, float deltaDepth, float depthMargin, float fftWindowSizeInPulseLength) {
      float pulseLength = broadbandData.getSoundVelocity() * broadbandData.getPulseDuration();
      float fftWindowRadius = fftWindowSizeInPulseLength * pulseLength / 2;

      // Make sure that the center of each depth cell sees +-fftWindowRadius:
      depthRange = depthRange.intersection(broadbandData.getDepthRange().shrink(fftWindowRadius - deltaDepth / 2));

      int n = (int) Math.floor((depthRange.getSize() - 2 * depthMargin + deltaDepth) / deltaDepth);
      if (n <= 0) {
         return List.of();
      }
      float firstDepthCenter = depthRange.min() + (depthRange.getSize() - n * deltaDepth + deltaDepth) / 2;
      return IntStream.range(0, n)
            .mapToObj(i -> FloatRange.ofCenterAndRadius(firstDepthCenter + i * deltaDepth, fftWindowRadius))
            .toList();
   }

   public float[] calculate(FloatRange depthRange, FloatRange frequencyRange) {
      int beginIndex = broadbandData.depthToClampedSampleIndex(depthRange.min());
      int endIndex = broadbandData.depthToClampedSampleIndex(depthRange.max());
      return calculate(beginIndex, endIndex, frequencyRange);
   }

   public float[] calculate(int beginIndex, int endIndex, FloatRange frequencyRange) {
      int nw = endIndex - beginIndex;
      int fftLength = calculateRequiredFftLength(nw);

      ComplexArray averagedPulseCompressedSignal = averagePulseCompressedSignal.copyOfRange(beginIndex, endIndex, fftLength);

      boolean useTVG = getUseTVG();

      if (useTVG) {
         for (int i = beginIndex; i < endIndex; i++) {
            float sampleRange = broadbandData.getSampleRange(i);
            averagedPulseCompressedSignal.multiply(i - beginIndex, sampleRange);
         }
      }

      double[] w = NORMALIZED_HANNING_WINDOW_CACHE.getUnchecked(nw);
      averagedPulseCompressedSignal.multiply(w, w.length);

      int centerIndex = (beginIndex + endIndex) / 2;

      ComplexArray reducedAutoCorrelatedSignal = reducedAutoCorrelatedSignalCache.getUnchecked(
            new ReducedAutoCorrelatedSignalCacheKey(beginIndex - centerIndex, endIndex - centerIndex, fftLength));

      DoubleFFT_1D fft = FftCache.getDouble1D(fftLength);
      fft.complexForward(averagedPulseCompressedSignal.values());

      int shift = (int) (shiftFactor * fftLength);

      IntRange indexRange = frequencyRangeToIndexRange(frequencyRange, fftLength);

      float[] sv = new float[indexRange.getSize()];

      // not frequency-dependent term
      double r = broadbandData.getTvgRange(centerIndex);
      double tenToTheTwoTimesRangeByTen = Math.pow(10, r / 5); // = 10^(2 * r * alpha / 10) = (10^(r / 5))^alpha
      double tw = nw * broadbandData.getSampleInterval();
      double constantTerm = PowerData.IMR_CONSTANT * (32 * Math.PI * Math.PI) / (broadbandData.getTransmitPower() * broadbandData.getSoundVelocity() * tw);

      FrequencyTerm frequencyTerm = frequencyTermCache.getUnchecked(new FrequencyTermCacheKey(indexRange.begin(), indexRange.end(), fftLength));

      for (int i = indexRange.begin(); i < indexRange.end(); i++) {
         int iShifted = (i + shift) % fftLength;

         // Divide by FFT of transmitted signal and compute absolute value squared.
         // |(a + ib)/(c + id)|^2 = |a + ib|^2 / |c + id|^2
         double prx = averagedPulseCompressedSignal.abs2(iShifted) / reducedAutoCorrelatedSignal.abs2(iShifted);

         sv[i - indexRange.begin()] = (float) (prx
               * (useTVG ? Math.pow(tenToTheTwoTimesRangeByTen, frequencyTerm.absorptionValues[i]) : 1)
               * frequencyTerm.values[i]
               * constantTerm);
      }

      return sv;
   }

   private record ReducedAutoCorrelatedSignalCacheKey(int beginIndexRelPeak, int endIndexRelPeak, int fftLength) {
   }

   private record FrequencyTermCacheKey(int beginIndex, int endIndex, int fftLength) {
   }

   private record FrequencyTerm(double[] values, double[] absorptionValues) {
   }

   private FrequencyTerm computeFrequencyTerm(int beginIndex, int endIndex, int fftLength) {
      IntToDoubleFunction frequencies = getFrequencies(fftLength);
      double[] values = new double[endIndex];
      double[] absorptionValues = new double[endIndex];
      for (int i = beginIndex; i < endIndex; i++) {
         double f = frequencies.applyAsDouble(i);
         double fNonZero = f > 0 ? f : frequencies.applyAsDouble(1); // f for division, avoids f=0
         double lambda = broadbandData.getSoundVelocity() / fNonZero;
         double psi = broadbandData.getPsi(fNonZero);
         double g = broadbandData.getGain(fNonZero);
         double prxFactor = broadbandData.getPrxFactor(fNonZero);
         values[i] = prxFactor * Math.pow(10, -(2 * g + psi) / 10) / (lambda * lambda);
         absorptionValues[i] = absorption.getAbsorption(f);
      }
      return new FrequencyTerm(values, absorptionValues);
   }
}
