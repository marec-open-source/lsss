package no.imr.korona.computation.broadband;

import no.imr.korona.computation.broadband.transferfunction.IdentityTransferFunction;
import no.imr.korona.computation.broadband.transferfunction.TransferFunction;
import no.imr.korona.data.ping.items.configuration.PulseForm;
import no.imr.tools.ShouldNotHappenException;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.math.ComplexArrayUtils;
import no.imr.tools.range.FloatRange;

import java.util.Arrays;

public final class PulseCompression {
   public static final PulseCompression EMPTY = new PulseCompression();

   private final PulseCompressionConfig config;
   private final ComplexArray autoCorrelationTransmitSignal;
   private final double normSq;
   private final double tauEff;
   private final ComplexArray matchedFilter;

   private PulseCompression() {
      config = new PulseCompressionConfig(0, 0, FloatRange.EMPTY_RANGE, 0, 0,
            PulseCompressionFilterChain.EMPTY, new IdentityTransferFunction(), new IdentityTransferFunction());
      autoCorrelationTransmitSignal = ComplexArray.EMPTY;
      normSq = 0;
      tauEff = 0;
      matchedFilter = ComplexArray.EMPTY;
   }

   PulseCompression(PulseCompressionConfig config) {
      this.config = config;
      double[] fullSentSignal = generateFullSentSignal(config.slope(), config.pulseDuration(),
            config.originalSamplingRate(), config.frequencyRange(), config.pulseForm());
      ComplexArray signal = config.filterChain().apply(config.signalTransferFunction().applyToTimeSignal(fullSentSignal, config.originalSamplingRate(), 0));
      ComplexArray flippedSignal = ComplexArrayUtils.flippedAndConjugated(signal);
      normSq = ComplexArrayUtils.normSq(signal);
      autoCorrelationTransmitSignal = computeAutoCorrelationTransmitSignal(signal, flippedSignal, normSq);
      tauEff = calculateEffectiveTau(autoCorrelationTransmitSignal, config.originalSamplingRate() / config.filterChain().getTotalDecimationFactor());

      TransferFunction totalTransferFunction = config.signalTransferFunction().multiply(config.responseTransferFunction());
      ComplexArray matchedFilterSignal = config.filterChain().apply(totalTransferFunction.applyToTimeSignal(fullSentSignal, config.originalSamplingRate(), 0));
      matchedFilter = ComplexArrayUtils.flippedAndConjugated(matchedFilterSignal);
   }

   public static double[] generateWtx(double slope, int n) {
      int nwtxh = (int) Math.floor(slope * n); // Length of half hanning window
      double[] wtxtmp = symHanning(2 * nwtxh); // hanning with selected slope

      double[] wtx = new double[n];
      System.arraycopy(wtxtmp, 0, wtx, 0, nwtxh);
      Arrays.fill(wtx, nwtxh, n - nwtxh, 1.0);
      System.arraycopy(wtxtmp, nwtxh, wtx, n - nwtxh, nwtxh);
      return wtx;
   }

   public static double[] generateFullSentSignal(float slope, float pulseDuration, double samplingFrequency, FloatRange frequencyRange, int pulseForm) {
      float fStart;
      float fStop;

      switch (pulseForm) {
         case PulseForm.NARROWBAND, PulseForm.BROADBAND_LINEAR_UP -> {
            fStart = frequencyRange.min();
            fStop = frequencyRange.max();
         }
         case PulseForm.BROADBAND_LINEAR_DOWN -> {
            fStart = frequencyRange.max();
            fStop = frequencyRange.min();
         }
         default -> {
            throw new ShouldNotHappenException("Unknown pulse form: " + pulseForm);
         }
      }

      double dt = 1 / samplingFrequency;
      double[] t = generateTimeVector(dt, pulseDuration);
      double[] wtx = generateWtx(slope, t.length);
      double[] yc = generateLinearChirp(t, fStart, pulseDuration, fStop);
      return windowAndScaleSignal(yc, wtx);
   }

   private static double[] generateTimeVector(double dt, double pulseDuration) {
      int n = (int) Math.round(pulseDuration / dt);
      double[] t = new double[n];
      for (int i = 0; i < t.length; i++) {
         t[i] = i * dt;
      }
      return t;
   }

   private static double[] windowAndScaleSignal(double[] yc, double[] wtx) {
      double[] y = new double[yc.length];
      double max = 0;
      for (int i = 0; i < yc.length; i++) {
         double re = yc[i] * wtx[i];
         max = Math.max(max, Math.abs(re));
         y[i] = re;
      }
      ArrayMath.divide(y, max);
      return y;
   }

   private static double[] generateLinearChirp(double[] t, double f0, double t1, double f1) {
      double[] y = new double[t.length];
      double beta = (f1 - f0) / t1;
      for (int i = 0; i < y.length; i++) {
         double ti = t[i];
         double fi = f0 + beta * ti / 2;
         y[i] = Math.cos(2 * Math.PI * fi * ti);
      }
      return y;
   }

   public static double calculateEffectiveTau(ComplexArray autoCorrelationTransmitSignal, float decimatedSamplingFrequency) {
      ComplexArray ytxa = autoCorrelationTransmitSignal;

      double sptx = 0;
      double ptxMax = 0;

      for (int i = 0; i < ytxa.length(); i++) {
         double ptxa = ytxa.abs2(i);
         sptx += ptxa;
         ptxMax = Math.max(ptxMax, ptxa);
      }

      return sptx / (ptxMax * decimatedSamplingFrequency);
   }

   private static ComplexArray computeAutoCorrelationTransmitSignal(ComplexArray signal, ComplexArray flippedSignal, double normSq) {
      ComplexArray avgTx = ComplexArrayUtils.conv(signal, flippedSignal);
      avgTx.divide(normSq);
      return avgTx;
   }

   public ComplexArray getAutoCorrelationTransmitSignal() {
      return autoCorrelationTransmitSignal;
   }

   public ComplexArray computePulseCompressedSignal(ComplexArray values) {
      ComplexArray result = ComplexArrayUtils.conv(values, matchedFilter, 0, values.length());
      result.divide(normSq);
      return result;
   }

   static double[] symHanning(int pointCount) {
      double[] w = new double[pointCount];
      double f = 2 * Math.PI / (pointCount - 1);
      for (int i = 0, j = pointCount - 1; i <= j; i++, j--) {
         w[i] = w[j] = 0.5 * (1.0 - Math.cos(i * f));
      }
      return w;
   }

   public PulseCompressionConfig getConfig() {
      return config;
   }

   public double getTauEff() {
      return tauEff;
   }

   public int getConvolutionKernelSize() {
      return matchedFilter.length();
   }
}
