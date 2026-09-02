package no.imr.korona.computation.broadband.transferfunction;

import no.imr.korona.computation.broadband.notchfilter.BroadbandNotchFilterConfig;
import no.imr.tools.math.MathUtils;
import no.imr.tools.range.FloatRange;

import java.util.function.DoubleUnaryOperator;

/**
 * Helper class to generate transfer functions / filters.
 * <p>
 * Contains some standard filters defined in
 * Analog Devices Inc. (2008). Chapter 8: Analog Filters. I H. Zumbahlen, Linear Circuit Design Handbook (p 583-679). London: Newnes.
 * <p>
 * Also contains some third order Butterworth filters.
 */
public final class TransferFunctionUtils {
   private TransferFunctionUtils() {
   }

   public static TransferFunction generateBandPassFilter(double startFrequency, double stopFrequency) {
      //             w / q * z
      // H(z)= ---------------------
      //       z^2 + w / q * z + w^2
      //
      // with q = w / (stopFrequency - startFrequency)
      // w = sqrt(stopFrequency * startFrequency)
      // stopFrequency startFrequency are the frequencies where the frequency response has dropped 3 dB from the maximum at w
      double w = Math.sqrt(startFrequency * stopFrequency);
      double q = w / (stopFrequency - startFrequency);
      return new LinearTransferFunction(new double[]{0, w / q}, new double[]{w * w, w / q, 1});
   }

   public static TransferFunction generateBandPassFilter(FloatRange frequencyRange) {
      return generateBandPassFilter(frequencyRange.min(), frequencyRange.max());
   }

   public static TransferFunction generateLowPassFilter(double stopFrequency, double q) {
      //                1
      // H(z)= ---------------------
      //       z^2 + w / q * z + w^2
      //
      // with w = stopFrequency
      return new LinearTransferFunction(new double[]{1}, new double[]{stopFrequency * stopFrequency, stopFrequency / q, 1});
   }

   public static LinearTransferFunction generateButterworthLowPassFilter(double stopFrequency) {
      // 3rd degree Butterworth filter
      //                         w^3
      // H(z)= -----------------------------------
      //       z^3 + 2 * w * z^2 + 2 * w^2 * z + w^3
      //
      // with w = stopFrequency
      return new LinearTransferFunction(new double[]{Math.pow(stopFrequency, 3)},
            new double[]{Math.pow(stopFrequency, 3), 2 * stopFrequency * stopFrequency, 2 * stopFrequency, 1});
   }

   public static LinearTransferFunction generateButterworthHighPassFilter(double startFrequency) {
      // H_hp = H_lp(1/z)
      // where H_lp is the Butterworth low-pass filter
      return new LinearTransferFunction(new double[]{0, 0, 0, 1},
            new double[]{Math.pow(startFrequency, 3), 2 * startFrequency * startFrequency, 2 * startFrequency, 1});
   }

   public static TransferFunction generateButterworthCascadingBandPassFilter(double startFrequency, double stopFrequency) {
      // bandpass as combination of low- and high-pass Butterworth filter
      return generateButterworthLowPassFilter(stopFrequency).multiply(generateButterworthHighPassFilter(startFrequency));
   }

   public static TransferFunction generateButterworthCascadingBandPassFilter(FloatRange frequencyRange) {
      return generateButterworthCascadingBandPassFilter(frequencyRange.min(), frequencyRange.max());
   }

   public static TransferFunction generateHighPassFilter(double startFrequency, double q) {
      //                z^2
      // H(z)= ---------------------
      //       z^2 + w / q * z + w^2
      //
      // with w = startFrequency
      return new LinearTransferFunction(new double[]{0, 0, 1}, new double[]{startFrequency * startFrequency, startFrequency / q, 1});
   }

   public static LinearTransferFunction generateNotchFilter(double rejectionFrequency, double q) {
      //             z^2 + w^2
      // z  -> ---------------------
      //       z^2 + w / q * z + w^2
      return new LinearTransferFunction(new double[]{rejectionFrequency * rejectionFrequency, 0, 1},
            new double[]{rejectionFrequency * rejectionFrequency, rejectionFrequency / q, 1});
   }

   public static LinearTransferFunction generateNotchFilter(BroadbandNotchFilterConfig notchFilterConfig, FloatRange frequencyRange) {
      int iter = 0;
      int iterMax = 1000;

      double qMin = 1e-4;
      double qMax = 1e4;

      while (true) {
         double q = (qMin + qMax) / 2;
         LinearTransferFunction transferFunction = generateNotchFilter(notchFilterConfig.rejectionFrequency(), q);
         double current3DbBandwidth = find3DbDropBandwidth(transferFunction, notchFilterConfig.rejectionFrequency(), frequencyRange);
         if (Math.abs(current3DbBandwidth - notchFilterConfig.bandwidth()) <= notchFilterConfig.bandwidth() * 0.001 || ++iter > iterMax) {
            return transferFunction;
         }
         if (current3DbBandwidth > notchFilterConfig.bandwidth()) {
            qMin = q;
         } else {
            qMax = q;
         }
      }
   }

   static TransferFunction generateNotchFilterFromConfig(NotchFilterConfig config) {
      TransferFunction transferFunction = new IdentityTransferFunction();
      for (BroadbandNotchFilterConfig broadbandNotchFilterConfig : config.broadbandNotchFilterConfigs()) {
         transferFunction = transferFunction.multiply(generateNotchFilter(broadbandNotchFilterConfig, config.frequencyRange()));
      }
      return transferFunction;
   }

   private static double find3DbDropBandwidth(TransferFunction transferFunction, double rejectionFrequency, FloatRange frequencyRange) {
      DoubleUnaryOperator function = frequency -> transferFunction.evaluateGainFunctionInDb(frequency) + 3;
      double a = function.applyAsDouble(frequencyRange.min()) <= 0
            ? frequencyRange.min()
            : MathUtils.findRoot(frequencyRange.min(), rejectionFrequency, function, 1e-3, 1e-3);
      double b = function.applyAsDouble(frequencyRange.max()) <= 0
            ? frequencyRange.max()
            : MathUtils.findRoot(rejectionFrequency, frequencyRange.max(), function, 1e-3, 1e-3);
      return b - a;
   }
}
