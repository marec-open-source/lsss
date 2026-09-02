package no.imr.tools.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ComplexArrayTest {
   @Test
   void of() {
      assertEquals(1, ComplexArray.of(new double[]{0, 0}).length());
      assertThrows(IllegalArgumentException.class, () -> ComplexArray.of(new double[]{0, 0, 0}));
      assertThrows(IllegalArgumentException.class, () -> ComplexArray.of(new double[]{0}, new double[]{0, 0}));
   }

   @Test
   void ofReal() {
      ComplexArray complexArray = ComplexArray.ofReal(new double[]{1, 2});
      assertEquals(1, complexArray.re(0));
      assertEquals(0, complexArray.im(0));
      assertEquals(2, complexArray.re(1));
      assertEquals(0, complexArray.im(1));
   }

   @Test
   void arg() {
      assertEquals(0, ComplexArray.of(new double[]{1, 0}).arg(0));
      assertEquals(Math.toRadians(45), ComplexArray.of(new double[]{1, 1}).arg(0));
      assertEquals(Math.toRadians(90), ComplexArray.of(new double[]{0, 1}).arg(0));
      assertEquals(Math.toRadians(-90), ComplexArray.of(new double[]{0, -1}).arg(0));
      assertEquals(Math.toRadians(180), ComplexArray.of(new double[]{-1, 0}).arg(0));
   }

   @Test
   void abs() {
      assertEquals(0, ComplexArray.of(new double[]{0, 0}).abs(0));
      assertEquals(1, ComplexArray.of(new double[]{1, 0}).abs(0));
      assertEquals(5, ComplexArray.of(new double[]{3, 4}).abs(0));
   }

   @Test
   void abs2() {
      assertEquals(0, ComplexArray.of(new double[]{0, 0}).abs2(0));
      assertEquals(1, ComplexArray.of(new double[]{1, 0}).abs2(0));
      assertEquals(25, ComplexArray.of(new double[]{3, 4}).abs2(0));
   }

   @Test
   void copyOfRange() {
      ComplexArray arr = ComplexArray.of(new double[]{1, -1, 2, -2, 3, -3, 4, -4, 5, -5});
      assertEquals(ComplexArray.of(new double[]{2, -2, 3, -3, 0, 0}), arr.copyOfRange(1, 3, 3));
      assertEquals(ComplexArray.of(new double[]{4, -4, 5, -5, 0, 0}), arr.copyOfRange(3, 5, 3));
      assertEquals(ComplexArray.of(new double[]{4, -4}), arr.copyOfRange(3, 5, 1));

      assertThrows(IndexOutOfBoundsException.class, () -> arr.copyOfRange(-1, 3, 3));  // negative beginIndex
      assertThrows(IndexOutOfBoundsException.class, () -> arr.copyOfRange(3, 2, 3));   // beginIndex > endIndex
      assertThrows(IndexOutOfBoundsException.class, () -> arr.copyOfRange(0, 6, 3));   // endIndex > length
      assertThrows(NegativeArraySizeException.class, () -> arr.copyOfRange(0, 3, -1));  // negative newLength
   }

   @Test
   void add() {
      ComplexArray a = ComplexArray.of(new double[]{1, 2, -3, 4});
      ComplexArray b = ComplexArray.of(new double[]{4, -1, 1, 2});
      a.add(b);
      assertArrayEquals(new double[]{5, 1, -2, 6}, a.values());
   }

   @Test
   void multiplyByFactor() {
      ComplexArray a = ComplexArray.of(new double[]{1, 2, -3, 4});
      a.multiply(2);
      assertArrayEquals(new double[]{2, 4, -6, 8}, a.values());
   }

   @Test
   void multiply() {
      ComplexArray a = ComplexArray.of(new double[]{1, 0, 0, 1});
      ComplexArray b = ComplexArray.of(new double[]{1, 0, 0, 1});
      a.multiply(b);
      assertArrayEquals(new double[]{1, 0, -1, 0}, a.values());
   }

   @Test
   void divide() {
      ComplexArray a = ComplexArray.of(new double[]{2, 4, -6, 8});
      a.divide(2);
      assertArrayEquals(new double[]{1, 2, -3, 4}, a.values());
   }
}
