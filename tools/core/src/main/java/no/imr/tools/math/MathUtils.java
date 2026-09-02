package no.imr.tools.math;

import com.google.common.math.BigIntegerMath;
import org.apache.commons.numbers.rootfinder.BrentSolver;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.function.BinaryOperator;
import java.util.function.DoubleUnaryOperator;

public final class MathUtils {
   private MathUtils() {
   }

   /// {@return the acos of the value clamped to `[-1, 1]`}
   ///
   /// Clamping might be necessary for computed values where
   /// numerical inaccuracies push it slightly outside `[-1, 1]`.
   public static double acosClamped(double value) {
      return Math.acos(Math.clamp(value, -1, 1));
   }

   /// {@return the asin of the value clamped to `[-1, 1]`}
   ///
   /// Clamping might be necessary for computed values where
   /// numerical inaccuracies push it slightly outside `[-1, 1]`.
   public static double asinClamped(double value) {
      return Math.asin(Math.clamp(value, -1, 1));
   }

   public static double sq(double x) {
      return x * x;
   }

   public static float hypot(float x, float y) {
      // Math.hypot is a lot slower than this.
      return (float) Math.sqrt(x * x + y * y);
   }

   public static double hypot(double x, double y) {
      // Math.hypot is a lot slower than this.
      return Math.sqrt(x * x + y * y);
   }

   /**
    * Returns the common residue, which is non-negative, of
    * <blockquote><pre>
    * value (mod modulus).
    * </pre></blockquote>
    * Note that a % m &lt;= 0 if a &lt;= 0.
    *
    * @param value   the value
    * @param modulus the modulus &gt; 0
    * @return the remainder in [0, modulus)
    * @throws IllegalArgumentException if modulus &lt;= 0;
    */
   public static int mod(int value, int modulus) {
      if (modulus <= 0) {
         throw new IllegalArgumentException("Non-positive modulus " + modulus);
      }
      int result = value % modulus;
      return result >= 0 ? result : result + modulus;
   }

   public static double mod(double value, double modulus) {
      if (modulus <= 0) {
         throw new IllegalArgumentException("Non-positive modulus " + modulus);
      }
      double result = value % modulus;
      if (result <= 0) { // Also catches result = -0.0.
         result += modulus;
         if (result >= modulus) {
            // Can happen for small negative values because of floating point rounding.
            result = 0;
         }
      }
      return result;
   }

   public static float round(float value, float roundingFactor) {
      return Float.isFinite(value) ? Math.round(value * roundingFactor) / roundingFactor : value;
   }

   public static double round(double value, double roundingFactor) {
      return Double.isFinite(value) ? Math.round(value * roundingFactor) / roundingFactor : value;
   }

   public static double roundToNumberOfDigits(double value, int numberOfDigits) {
      if (numberOfDigits < 1) {
         throw new IllegalArgumentException(Integer.toString(numberOfDigits));
      }
      if (value == 0 || !Double.isFinite(value)) {
         return value;
      }
      BigDecimal bigDecimal = BigDecimal.valueOf(value);
      BigInteger unscaledValue = bigDecimal.unscaledValue().abs();
      int digits = BigIntegerMath.log10(unscaledValue, RoundingMode.FLOOR) + 1;
      return bigDecimal.setScale(bigDecimal.scale() - digits + numberOfDigits, RoundingMode.HALF_UP).doubleValue();
   }

   public static double interpolate(double valueA, double valueB, double fractionFromAToB) {
      return valueA + fractionFromAToB * (valueB - valueA);
   }

   public static double interpolateDegrees(double degA, double degB, double fractionFromAToB) {
      degA = normalizeAngle0To360(degA);
      degB = normalizeAngle0To360(degB);
      if (Math.abs(degB - degA) > 180) {
         if (degA < degB) {
            degA += 360;
         } else {
            degB += 360;
         }
      }
      double result = interpolate(degA, degB, fractionFromAToB);
      return normalizeAngle0To360(result);
   }

   public static double normalizeAngle0To360(double angle) {
      return mod(angle, 360);
   }

   public static double findRoot(double x1, double x2, DoubleUnaryOperator f, double xAccuracy, double yAccuracy) {
      return new BrentSolver(0, xAccuracy, yAccuracy).findRoot(f, x1, x2);
   }

   public static <T> T pow(T value, BinaryOperator<T> multiply, long exponent) {
      if (exponent <= 0) {
         throw new IllegalArgumentException("Non-positive exponent: " + exponent);
      }
      T result = null;
      while (true) {
         if ((exponent & 1) != 0) {
            result = result != null ? multiply.apply(result, value) : value;
            if (exponent == 1) {
               return result;
            }
         }
         value = multiply.apply(value, value);
         exponent >>>= 1;
      }
   }

   public static float avoidInfinity(float value) {
      if (Float.isInfinite(value)) {
         return Math.signum(value) * Float.MAX_VALUE;
      }
      return value;
   }

   /**
    * Remove infinite values.
    *
    * @param values an array with some possibly infinite values
    * @return true if infinities were found and removed
    */
   public static boolean avoidInfinities(float[] values) {
      boolean foundInfinity = false;
      for (int i = 0; i < values.length; i++) {
         if (Float.isInfinite(values[i])) {
            foundInfinity = true;
            values[i] = avoidInfinity(values[i]);
         }
      }
      return foundInfinity;
   }
}
