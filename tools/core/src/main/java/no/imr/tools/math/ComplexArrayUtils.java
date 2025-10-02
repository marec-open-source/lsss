package no.imr.tools.math;

import com.google.common.math.IntMath;
import org.apache.commons.numbers.complex.Complex;
import org.jtransforms.fft.DoubleFFT_1D;

public final class ComplexArrayUtils {
   private ComplexArrayUtils() {
   }

   public static double normSq(ComplexArray values) {
      double norm2 = 0;
      for (int i = 0; i < values.length(); i++) {
         norm2 += values.abs2(i);
      }
      return norm2;
   }

   public static double maxAbs(ComplexArray values) {
      double maxAbs2 = 0;
      for (int i = 0; i < values.length(); i++) {
         maxAbs2 = Math.max(maxAbs2, values.abs2(i));
      }
      return Math.sqrt(maxAbs2);
   }

   public static int maxIndex(ComplexArray values) {
      return maxIndex(values, 0, values.length());
   }

   public static int maxIndex(ComplexArray values, int beginIndex, int endIndex) {
      int iMax = -1;
      double valueMax = Double.NEGATIVE_INFINITY;
      for (int i = beginIndex; i < endIndex; i++) {
         double value = values.abs2(i);
         if (value > valueMax) {
            iMax = i;
            valueMax = value;
         }
      }
      return iMax;
   }

   public static ComplexArray downsample(ComplexArray values, int decimation) {
      int n = (values.length() + decimation - 1) / decimation;
      ComplexArray downsampledValues = ComplexArray.ofLength(n);
      for (int i = 0, j = 0; i < n; i++, j += decimation) {
         downsampledValues.set(i, values, j);
      }
      return downsampledValues;
   }

   public static ComplexArray flippedAndConjugated(ComplexArray values) {
      ComplexArray flippedValues = ComplexArray.ofLength(values.length());
      for (int i = 0, j = values.length() - 1; i < values.length(); i++, j--) {
         flippedValues.setConjugated(i, values, j);
      }
      return flippedValues;
   }

   public static ComplexArray conv(ComplexArray firstArray, ComplexArray secondArray) {
      /*
       * High-level convolution of the two complex arrays by FFT
       * returns an array with length of firstArray
       * padding is handled automatically
       */
      return conv(firstArray, secondArray, 0, firstArray.length());
   }

   public static ComplexArray convFull(ComplexArray firstArray, ComplexArray secondArray) {
      int n = firstArray.length() + secondArray.length() - 1;
      int fftSize = IntMath.ceilingPowerOfTwo(n);
      DoubleFFT_1D fft = FftCache.getDouble1D(fftSize);
      return conv(firstArray, secondArray, fft, fftSize, 0, n);
   }

   public static ComplexArray conv(ComplexArray firstArray, ComplexArray secondArray, int beginIndex, int endIndex) {
      /*
       * High-level convolution of a sub-range of firstArray with secondArray
       * returns an array of length endIndex - beginIndex
       * padding is handled automatically
       */
      int n = firstArray.length() + secondArray.length() - 1;
      int offset = secondArray.length() / 2;

      int fftSize = IntMath.ceilingPowerOfTwo(n);
      DoubleFFT_1D fft = FftCache.getDouble1D(fftSize);
      return conv(firstArray, secondArray, fft, fftSize, beginIndex + offset, offset + endIndex);
   }

   private static ComplexArray conv(ComplexArray firstArray, ComplexArray secondArray, DoubleFFT_1D fft, int fftSize, int beginIndex, int endIndex) {
      /*
       * Low-level convolution with full control of padding, fft engine
       * and selection of sub-range
       */
      ComplexArray firstArrayPadded = firstArray.copyOf(fftSize);
      ComplexArray secondArrayPadded = secondArray.copyOf(fftSize);
      fft.complexForward(firstArrayPadded.values());
      fft.complexForward(secondArrayPadded.values());

      // Reusing firstArrayPadded to compute firstArrayPadded(i) = firstArrayPadded(i) * secondArrayPadded(i)
      firstArrayPadded.multiply(secondArrayPadded);

      fft.complexInverse(firstArrayPadded.values(), true);

      return firstArrayPadded.copyOfRange(beginIndex, endIndex);
   }

   public static Complex average(Complex a, Complex b) {
      double re = (a.getReal() + b.getReal()) / 2;
      double im = (a.getImaginary() + b.getImaginary()) / 2;
      return Complex.ofCartesian(re, im);
   }

   public static Complex average(ComplexArray complexArray) {
      double sumRe = 0;
      double sumIm = 0;
      double[] values = complexArray.values();
      int n = complexArray.length();
      for (int i = 0; i < n; i++) {
         sumRe += values[2 * i];
         sumIm += values[2 * i + 1];
      }
      return Complex.ofCartesian(sumRe / n, sumIm / n);
   }

   public static ComplexArray matchFilterArray(ComplexArray complexArray, ComplexArray complexFilter) {
      ComplexArray flippedAndConjugatedFilter = flippedAndConjugated(complexFilter);
      flippedAndConjugatedFilter.divide(normSq(flippedAndConjugatedFilter));
      return conv(complexArray, flippedAndConjugatedFilter, 0, complexArray.length());
   }
}
