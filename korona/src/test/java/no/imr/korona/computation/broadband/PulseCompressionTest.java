package no.imr.korona.computation.broadband;

import no.imr.tools.math.ComplexArray;
import no.imr.tools.math.ComplexArrayUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testing pulse compression (for broadband complex data).
 */
final class PulseCompressionTest {
   private static ComplexArray convBruteForce(ComplexArray firstArray, ComplexArray secondArray, int beginIndex, int endIndex) {
      ComplexArray result = ComplexArray.ofLength(endIndex - beginIndex);

      int m = secondArray.length() / 2;
      for (int i = beginIndex; i < endIndex; i++) {
         double re = 0;
         double im = 0;
         for (int j = 0; j < secondArray.length(); j++) {
            if (i - j + m < 0 || i - j + m >= firstArray.length()) {
               continue;
            }
            double re1 = firstArray.re(i - j + m);
            double im1 = firstArray.im(i - j + m);
            double re2 = secondArray.re(j);
            double im2 = secondArray.im(j);
            re += re1 * re2 - im1 * im2;
            im += im1 * re2 + re1 * im2;
         }
         result.set(i - beginIndex, re, im);
      }
      return result;
   }

   @Test
   void testConvolution() {
      ComplexArray array1 = ComplexArray.ofLength(16);
      for (int i = 0; i < array1.length(); i++) {
         double re = 0.1 * i;
         double im = -0.5 + 0.05 * i;
         array1.set(i, re, im);
      }
      ComplexArray array2 = ComplexArray.ofLength(100);
      for (int i = 0; i < array2.length(); i++) {
         double re = 2 * Math.cos(2 * Math.PI * (double) i / array2.length());
         double im = Math.sin(2 * Math.PI * (double) i / array2.length());
         array2.set(i, re, im);
      }

      conv(array1, array2);
      conv(array2, array1);
   }

   private static void conv(ComplexArray array1, ComplexArray array2) {
      ComplexArray result1 = convBruteForce(array1, array2, 0, array1.length());
      ComplexArray result2 = ComplexArrayUtils.conv(array1, array2);

      assertArrayEquals(result1.values(), result2.values(), 1e-5);
   }

   @Test
   void symHanning() {
      assertArrayEquals(new double[]{0, 0.5, 1, 0.5, 0},
            PulseCompression.symHanning(5), 1e-16);
      assertArrayEquals(new double[]{0, 0.1882550990706332, 0.6112604669781572, 0.9504844339512095, 0.9504844339512095, 0.6112604669781572, 0.1882550990706332, 0},
            PulseCompression.symHanning(8));
   }
}
