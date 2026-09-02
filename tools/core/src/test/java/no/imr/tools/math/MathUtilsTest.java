package no.imr.tools.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class MathUtilsTest {
   @Test
   void acosClamped() {
      assertEquals(Math.PI, MathUtils.acosClamped(-1.000000001));
      assertEquals(Math.acos(-0.999), MathUtils.acosClamped(-0.999));
      assertEquals(Math.acos(0.999), MathUtils.acosClamped(0.999));
      assertEquals(0, MathUtils.acosClamped(1.000000001));
   }

   @Test
   void asinClamped() {
      assertEquals(-Math.PI / 2, MathUtils.asinClamped(-1.000000001));
      assertEquals(Math.asin(-0.999), MathUtils.asinClamped(-0.999));
      assertEquals(Math.asin(0.999), MathUtils.asinClamped(0.999));
      assertEquals(Math.PI / 2, MathUtils.asinClamped(1.000000001));
   }

   @Test
   void modInt() {
      assertEquals(0, MathUtils.mod(10, 10));
      assertEquals(9, MathUtils.mod(9, 10));
      assertEquals(1, MathUtils.mod(1, 10));
      assertEquals(0, MathUtils.mod(0, 10));
      assertEquals(9, MathUtils.mod(-1, 10));
      assertEquals(1, MathUtils.mod(-9, 10));
      assertEquals(0, MathUtils.mod(-10, 10));

      for (int i = -3; i <= 3; i++) {
         assertEquals(0, MathUtils.mod(3 * i, 3));
         assertEquals(1, MathUtils.mod(3 * i + 1, 3));
         assertEquals(2, MathUtils.mod(3 * i - 1, 3));
      }
   }

   @Test
   void modDouble() {
      assertEquals(0, MathUtils.mod(10.0, 10.0));
      assertEquals(0, MathUtils.mod(-10.0, 10.0));
      assertEquals(1.1, MathUtils.mod(3.1, 2));
      assertEquals(1.1, MathUtils.mod(5.1, 2), 1e-15);
      for (int i = -3; i <= 3; i++) {
         assertEquals(1.1, MathUtils.mod(3 * i + 1.1, 3.0), 1e-15);
      }
      assertEquals(0, MathUtils.mod(-Math.ulp(360.0) / 10, 360.0));
   }

   @Test
   void roundFloat() {
      assertEquals(1.1f, MathUtils.round(1.11f, 10));
      assertEquals(Float.NaN, MathUtils.round(Float.NaN, 10));
      assertEquals(Float.NEGATIVE_INFINITY, MathUtils.round(Float.NEGATIVE_INFINITY, 10));
      assertEquals(Float.POSITIVE_INFINITY, MathUtils.round(Float.POSITIVE_INFINITY, 10));
   }

   @Test
   void roundDouble() {
      assertEquals(1.1, MathUtils.round(1.11, 10));
      assertEquals(Double.NaN, MathUtils.round(Double.NaN, 10));
      assertEquals(Double.NEGATIVE_INFINITY, MathUtils.round(Double.NEGATIVE_INFINITY, 10));
      assertEquals(Double.POSITIVE_INFINITY, MathUtils.round(Double.POSITIVE_INFINITY, 10));
   }

   @Test
   void roundToNumberOfDigits() {
      assertEquals(0, MathUtils.roundToNumberOfDigits(0, 1));

      assertEquals(100, MathUtils.roundToNumberOfDigits(111, 1));
      assertEquals(-100, MathUtils.roundToNumberOfDigits(-111, 1));

      assertEquals(110, MathUtils.roundToNumberOfDigits(111, 2));
      assertEquals(-110, MathUtils.roundToNumberOfDigits(-111, 2));

      assertEquals(111, MathUtils.roundToNumberOfDigits(111, 3));
      assertEquals(-111, MathUtils.roundToNumberOfDigits(-111, 3));

      assertEquals(111, MathUtils.roundToNumberOfDigits(111, 4));
      assertEquals(-111, MathUtils.roundToNumberOfDigits(-111, 4));

      assertEquals(1.23, MathUtils.roundToNumberOfDigits(1.23456, 3));
      assertEquals(-1.23, MathUtils.roundToNumberOfDigits(-1.23456, 3));

      assertEquals(1.235, MathUtils.roundToNumberOfDigits(1.23456, 4));
      assertEquals(-1.235, MathUtils.roundToNumberOfDigits(-1.23456, 4));

      assertEquals(1.235e6, MathUtils.roundToNumberOfDigits(1.2345e6, 4));
      assertEquals(-1.235e6, MathUtils.roundToNumberOfDigits(-1.2345e6, 4));

      assertEquals(Double.NaN, MathUtils.roundToNumberOfDigits(Double.NaN, 4));
      assertEquals(Double.NEGATIVE_INFINITY, MathUtils.roundToNumberOfDigits(Double.NEGATIVE_INFINITY, 4));
      assertEquals(Double.POSITIVE_INFINITY, MathUtils.roundToNumberOfDigits(Double.POSITIVE_INFINITY, 4));
   }

   @Test
   void interpolate() {
      assertEquals(-10, MathUtils.interpolate(0, 10, -1));
      assertEquals(0, MathUtils.interpolate(0, 10, 0));
      assertEquals(5, MathUtils.interpolate(0, 10, 0.5));
      assertEquals(10, MathUtils.interpolate(0, 10, 1));
      assertEquals(25, MathUtils.interpolate(0, 10, 2.5));
   }

   @Test
   void interpolateDegrees() {
      assertEquals(45, MathUtils.interpolateDegrees(0, 90, 0.5));
      assertEquals(90, MathUtils.interpolateDegrees(0, 90, 1));
      assertEquals(0, MathUtils.interpolateDegrees(45, 360 - 45, 0.5));
      assertEquals(1, MathUtils.interpolateDegrees(46, 360 - 44, 0.5));
      assertEquals(359, MathUtils.interpolateDegrees(44, 360 - 46, 0.5));

      assertEquals(355, MathUtils.interpolateDegrees(355, 5, 0));
      assertEquals(356, MathUtils.interpolateDegrees(355, 5, 0.1));
      assertEquals(0, MathUtils.interpolateDegrees(355, 5, 0.5));
      assertEquals(1, MathUtils.interpolateDegrees(355, 5, 0.6));
      assertEquals(4, MathUtils.interpolateDegrees(355, 5, 0.9));
      assertEquals(5, MathUtils.interpolateDegrees(355, 5, 1));

      assertEquals(270, MathUtils.interpolateDegrees(0, 90, -5));
      assertEquals(270, MathUtils.interpolateDegrees(0, 90, -1));
      assertEquals(0, MathUtils.interpolateDegrees(0, 90, 4));
      assertEquals(90, MathUtils.interpolateDegrees(0, 90, 5));
   }

   @Test
   void normalizeAngle0To360() {
      assertEquals(0, MathUtils.normalizeAngle0To360(0));
      assertEquals(0, MathUtils.normalizeAngle0To360(360));
      assertEquals(0, MathUtils.normalizeAngle0To360(-360));
      assertEquals(0.1, MathUtils.normalizeAngle0To360(0.1));
      assertEquals(359.9, MathUtils.normalizeAngle0To360(359.9));
      assertEquals(359.9, MathUtils.normalizeAngle0To360(-0.1));
      assertEquals(0, MathUtils.normalizeAngle0To360(-1e-20));
   }

   @Test
   void findRoot() {
      assertEquals(1, MathUtils.findRoot(0.33, 13.1, x -> x * x - 1, 1e-5, 1e-5), 1e-5);
      assertEquals(-1, MathUtils.findRoot(-100, 0.5, x -> x * x - 1, 1e-5, 1e-5), 1e-5);
   }

   @Test
   void pow() {
      assertThrows(IllegalArgumentException.class, () -> MathUtils.pow(1, Math::multiplyExact, -1));
      assertThrows(IllegalArgumentException.class, () -> MathUtils.pow(1, Math::multiplyExact, 0));
      for (int i = 1; i <= 39; i++) {
         assertEquals(Math.powExact(3L, i), MathUtils.pow(3L, Math::multiplyExact, i));
      }
      assertEquals(Math.pow(1.1, 49), MathUtils.pow(1.1, (x, y) -> x * y, 49), 1e-13);
   }

   @Test
   void avoidInfinity() {
      assertEquals(-1, MathUtils.avoidInfinity(-1));
      assertEquals(0, MathUtils.avoidInfinity(0));
      assertEquals(1, MathUtils.avoidInfinity(1));

      assertEquals(Float.MAX_VALUE, MathUtils.avoidInfinity(Float.MAX_VALUE));
      assertEquals(Float.MAX_VALUE, MathUtils.avoidInfinity(Float.POSITIVE_INFINITY));

      assertEquals(-Float.MAX_VALUE, MathUtils.avoidInfinity(-Float.MAX_VALUE));
      assertEquals(-Float.MAX_VALUE, MathUtils.avoidInfinity(Float.NEGATIVE_INFINITY));

      assertEquals(Float.NaN, MathUtils.avoidInfinity(Float.NaN));
   }
}
