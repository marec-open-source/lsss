package no.imr.tools.math;

import no.imr.tools.Utils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

final class WelfordsMethodTest {
   @Test
   void test() {
      double[] values = {-1.1, -0.1, 0, 0.1, 1};
      double mean = Arrays.stream(values).sum() / values.length;
      double var = Arrays.stream(values).map(v -> Utils.sq(v - mean)).sum() / (values.length - 1);

      WelfordsMethod w = new WelfordsMethod();
      Arrays.stream(values).forEach(w::update);

      assertEquals(values.length, w.getCount());
      assertEquals(mean, w.getMean());
      assertEquals(var, w.getVariance());
      assertEquals(Math.sqrt(var), w.getStdDev());
      assertEquals(Math.sqrt(var) / Math.sqrt(values.length), w.getStdErr());
   }

   @Test
   void merge() {
      double[] values = {-1.1, -0.1, 0, 0.1, 1};

      for (int n = 2; n <= values.length; n++) {
         double mean = Arrays.stream(values, 0, n).sum() / n;
         double var = Arrays.stream(values, 0, n).map(v -> Utils.sq(v - mean)).sum() / (n - 1);

         for (int i = 0; i <= n; i++) {
            WelfordsMethod w1 = new WelfordsMethod();
            WelfordsMethod w2 = new WelfordsMethod();
            Arrays.stream(values, 0, i).forEach(w1::update);
            Arrays.stream(values, i, n).forEach(w2::update);
            w1.update(w2);

            assertEquals(n, w1.getCount());
            assertEquals(mean, w1.getMean(), 1e-10);
            assertEquals(var, w1.getVariance(), 1e-10);
            assertEquals(Math.sqrt(var), w1.getStdDev(), 1e-10);
         }
      }
   }

   @Test
   void mergeWithEmpty() {
      WelfordsMethod w = new WelfordsMethod();
      w.update(new WelfordsMethod()); // This caused NaN until 2019-10-09
      w.update(0);
      w.update(0);
      assertEquals(2, w.getCount());
      assertEquals(0, w.getMean());
      assertEquals(0, w.getVariance());
      assertEquals(0, w.getStdDev());
   }
}
