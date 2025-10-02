package no.imr.tools.math.linalg;

import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

final class Matrix3Test {
   @Test
   void testRotation() {
      Vec3 axis = new Vec3(1, 0, 0);
      float angle = 54;
      Matrix3 a = Matrix3.createRotation(angle, axis);
      Matrix3 b = Matrix3.createRotation(-angle, axis);
      Matrix3 c = a.multiply(b);
      JUnitUtils.assertEquals(Matrix3.IDENTITY, c);
   }

   @Test
   void testScaling() {
      Matrix3 a = Matrix3.createScaling(new Vec3(1, 2, 4));
      Matrix3 b = Matrix3.createScaling(new Vec3(1, 0.5f, 0.25f));
      Matrix3 c = a.multiply(b);
      JUnitUtils.assertEquals(Matrix3.IDENTITY, c);
   }

   @Test
   void testMultiplyVec() {
      Matrix3 m = new Matrix3(
            1, 2, 3,
            5, 0, 0,
            6, 0, 0);

      Vec3 res = m.multiply(new Vec3(1, 1, 1));
      JUnitUtils.assertEquals(new Vec3(6, 5, 6), res);
   }

   @Test
   void testMinus() {
      Matrix3 a = new Matrix3(1, 2, 3, 4, 5, 6, 7, 8, 9);
      JUnitUtils.assertEquals(new Matrix3(0, 0, 0, 0, 0, 0, 0, 0, 0), a.minus(a));
      JUnitUtils.assertEquals(a, a.minus(new Matrix3(0, 0, 0, 0, 0, 0, 0, 0, 0)));
   }

   @Test
   void testNormalize() {
      Matrix3 a = new Matrix3(
            1, 2, 3,
            5, 0, 0,
            6, 0, 0);
      Matrix3 b = a.normalize();
      Matrix3 c = b.transpose();

      JUnitUtils.assertEquals(Matrix3.IDENTITY, b.multiply(c), 1e-6f);
   }

   @Test
   void testInverse() {
      Matrix3 a = new Matrix3(1, 2, 3, 0, 6, 7, 0, 0, 12);
      JUnitUtils.assertEquals(Matrix3.IDENTITY, a.multiply(a.inverse()));
      a = new Matrix3(2, 9, 1, 11, 7, 2, 3, 16, 1);
      JUnitUtils.assertEquals(Matrix3.IDENTITY, a.multiply(a.inverse()), 1e-6f);
   }

   @Test
   void testMultiplyGetComponent() {
      Matrix3 a = new Matrix3(1, 2, 3, 4, 5, 6, 7, 8, 9);
      Vec3 b = new Vec3(17, 18, 19);
      assertEquals(a.multiply(b).x(), a.multiplyGetX(b));
      assertEquals(a.multiply(b).y(), a.multiplyGetY(b));
      assertEquals(a.multiply(b).z(), a.multiplyGetZ(b));
   }

   @Test
   void testStoreRowWise() {
      Matrix3 a = new Matrix3(1, 2, 3, 4, 5, 6, 7, 8, 9);
      ByteBuffer byteBuffer = ByteBuffer.allocate(4 * 9);
      a.writeRowWise(byteBuffer);
      byteBuffer.flip();
      Matrix3 b = Matrix3.readRowWise(byteBuffer);
      assertEquals(a, b);
   }
}
