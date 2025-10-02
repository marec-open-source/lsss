package no.imr.tools.math;

import no.imr.tools.Utils;
import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

final class WelfordsMethod2DTest {
   @Test
   void test() {
      JUnitUtils.runWithRandom(random -> {
         int n = random.nextInt(2, 20);
         double[] x = random.doubles(n).toArray();
         double[] y = random.doubles(n).toArray();

         double meanX = Arrays.stream(x).sum() / n;
         double meanY = Arrays.stream(y).sum() / n;
         double covXX = Arrays.stream(x).map(v -> Utils.sq(v - meanX)).sum() / (n - 1);
         double covYY = Arrays.stream(y).map(v -> Utils.sq(v - meanY)).sum() / (n - 1);
         double covXY = IntStream.range(0, n).mapToDouble(i -> (x[i] - meanX) * (y[i] - meanY)).sum() / (n - 1);

         WelfordsMethod2D w = new WelfordsMethod2D();
         for (int i = 0; i < n; i++) {
            w.update(x[i], y[i]);
         }

         assertEquals(w.meanX(), meanX, 1e-10);
         assertEquals(w.meanY(), meanY, 1e-10);
         assertEquals(w.covXX(), covXX, 1e-10);
         assertEquals(w.covXY(), covXY, 1e-10);
         assertEquals(w.covYY(), covYY, 1e-10);
      });
   }
}
