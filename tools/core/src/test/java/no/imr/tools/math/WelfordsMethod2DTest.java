package no.imr.tools.math;

import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

final class WelfordsMethod2DTest {
   @Test
   void test() {
      WelfordsMethod2D w = new WelfordsMethod2D();
      assertEquals(0, w.getCount());
      assertEquals(Double.NaN, w.meanX());
      assertEquals(Double.NaN, w.meanY());
      assertEquals(Double.NaN, w.covXX());
      assertEquals(Double.NaN, w.covXY());
      assertEquals(Double.NaN, w.covYY());

      w.update(1, 1);
      assertEquals(1, w.getCount());
      assertEquals(1, w.meanX());
      assertEquals(1, w.meanY());
      assertEquals(Double.NaN, w.covXX());
      assertEquals(Double.NaN, w.covXY());
      assertEquals(Double.NaN, w.covYY());

      w.update(1, 1);
      assertEquals(2, w.getCount());
      assertEquals(1, w.meanX());
      assertEquals(1, w.meanY());
      assertEquals(0, w.covXX());
      assertEquals(0, w.covXY());
      assertEquals(0, w.covYY());
   }

   @Test
   void random() {
      JUnitUtils.runWithRandom(random -> {
         int n = random.nextInt(2, 20);
         double[] x = random.doubles(n).toArray();
         double[] y = random.doubles(n).toArray();

         double meanX = Arrays.stream(x).sum() / n;
         double meanY = Arrays.stream(y).sum() / n;
         double covXX = Arrays.stream(x).map(v -> MathUtils.sq(v - meanX)).sum() / (n - 1);
         double covYY = Arrays.stream(y).map(v -> MathUtils.sq(v - meanY)).sum() / (n - 1);
         double covXY = IntStream.range(0, n).mapToDouble(i -> (x[i] - meanX) * (y[i] - meanY)).sum() / (n - 1);

         WelfordsMethod2D w = new WelfordsMethod2D();
         for (int i = 0; i < n; i++) {
            w.update(x[i], y[i]);
         }

         assertEquals(meanX, w.meanX(), 1e-10);
         assertEquals(meanY, w.meanY(), 1e-10);
         assertEquals(covXX, w.covXX(), 1e-10);
         assertEquals(covXY, w.covXY(), 1e-10);
         assertEquals(covYY, w.covYY(), 1e-10);
      });
   }
}
