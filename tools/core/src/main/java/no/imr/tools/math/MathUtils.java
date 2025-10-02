package no.imr.tools.math;

import org.apache.commons.numbers.rootfinder.BrentSolver;

import java.util.function.BinaryOperator;

public final class MathUtils {
   private MathUtils() {
   }

   public static double findRoot(double x1, double x2, Function1D f, double xAccuracy, double yAccuracy) {
      return new BrentSolver(0, xAccuracy, yAccuracy).findRoot(f::eval, x1, x2);
   }

   public static <T> T pow(T value, BinaryOperator<T> multiply, long exponent) {
      if (exponent <= 0) {
         throw new IllegalArgumentException("Non-positive exponent: " + exponent);
      }
      T result = null;
      while (true) {
         if ((exponent & 1) != 0) {
            result = result != null ? multiply.apply(result, value) : value;
         }
         exponent >>>= 1;
         if (exponent == 0) {
            assert result != null;
            return result;
         }
         value = multiply.apply(value, value);
      }
   }
}
