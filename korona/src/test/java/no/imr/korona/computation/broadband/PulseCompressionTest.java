package no.imr.korona.computation.broadband;

import no.imr.tools.math.ComplexArray;
import no.imr.tools.math.ComplexArrayUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testing pulse compression (for broadband complex data).
 */
final class PulseCompressionTest {
   private static ComplexArray convBruteForce(ComplexArray array1, ComplexArray array2, int beginIndex, int endIndex) {
      ComplexArray result = ComplexArray.ofLength(endIndex - beginIndex);
      int m = array2.length() / 2;
      for (int i = beginIndex; i < endIndex; i++) {
         double re = 0;
         double im = 0;
         for (int i2 = 0; i2 < array2.length(); i2++) {
            int i1 = i - i2 + m;
            if (i1 < 0 || i1 >= array1.length()) {
               continue;
            }
            double re1 = array1.re(i1);
            double im1 = array1.im(i1);
            double re2 = array2.re(i2);
            double im2 = array2.im(i2);
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
      double deltaAngle = 2 * Math.PI / array2.length();
      for (int i = 0; i < array2.length(); i++) {
         double angle = i * deltaAngle;
         double re = 2 * Math.cos(angle);
         double im = Math.sin(angle);
         array2.set(i, re, im);
      }

      conv(array1, array2);
      conv(array2, array1);
   }

   private static void conv(ComplexArray array1, ComplexArray array2) {
      ComplexArray result1 = convBruteForce(array1, array2, 0, array1.length());
      ComplexArray result2 = ComplexArrayUtils.conv(array1, array2);

      assertArrayEquals(result1.values(), result2.values(), 1e-13);
   }

   @Test
   void symHanning() {
      assertArrayEquals(new double[]{0, 0.5, 1, 0.5, 0},
            PulseCompression.symHanning(5), 1e-16);
      assertArrayEquals(new double[]{0, 0.1882550990706332, 0.6112604669781572, 0.9504844339512095, 0.9504844339512095, 0.6112604669781572, 0.1882550990706332, 0},
            PulseCompression.symHanning(8));
   }
}
