package no.imr.tools.math.linalg;

import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

final class Matrix3Test {
   @Test
   void createRotationFromAngleAndAxis() {
      Vec3 axis = new Vec3(1, 0, 0);
      float angle = 54;
      Matrix3 a = verifyRotationMatrix(Matrix3.createRotation(angle, axis));
      Matrix3 b = verifyRotationMatrix(Matrix3.createRotation(-angle, axis));
      Matrix3 c = a.multiply(b);
      JUnitUtils.assertEquals(Matrix3.IDENTITY, c);
   }

   @Test
   void createRotationFromTwoDirections() {
      verifyRotationMatrix(Matrix3.createRotation(new Vec3(1, 0, 0), new Vec3(1, 0, 1e-9f)));
      verifyRotationMatrix(Matrix3.createRotation(new Vec3(1, 0, 0), new Vec3(0, 1, 0)));
      verifyRotationMatrix(Matrix3.createRotation(new Vec3(1, 1, 1), new Vec3(-1, -1, -1)));
   }

   private static Matrix3 verifyRotationMatrix(Matrix3 r) {
      JUnitUtils.assertEquals(Matrix3.IDENTITY, r.multiply(r.transpose()), 1e-6f);
      assertEquals(1, r.determinant(), 1e-6f);
      return r;
   }

   @Test
   void createScaling() {
      Matrix3 a = Matrix3.createScaling(new Vec3(1, 2, 4));
      Matrix3 b = Matrix3.createScaling(new Vec3(1, 0.5f, 0.25f));
      Matrix3 c = a.multiply(b);
      JUnitUtils.assertEquals(Matrix3.IDENTITY, c);
   }

   @Test
   void multiplyVec() {
      Matrix3 m = new Matrix3(
            1, 2, 3,
            5, 0, 0,
            6, 0, 0);

      Vec3 res = m.multiply(new Vec3(1, 1, 1));
      JUnitUtils.assertEquals(new Vec3(6, 5, 6), res);
   }

   @Test
   void minus() {
      Matrix3 a = new Matrix3(1, 2, 3, 4, 5, 6, 7, 8, 9);
      JUnitUtils.assertEquals(new Matrix3(0, 0, 0, 0, 0, 0, 0, 0, 0), a.minus(a));
      JUnitUtils.assertEquals(a, a.minus(new Matrix3(0, 0, 0, 0, 0, 0, 0, 0, 0)));
   }

   @Test
   void normalize() {
      Matrix3 a = new Matrix3(
            1, 2, 3,
            5, 0, 0,
            6, 0, 0);
      Matrix3 b = a.normalize();
      Matrix3 c = b.transpose();

      JUnitUtils.assertEquals(Matrix3.IDENTITY, b.multiply(c), 1e-6f);
   }

   @Test
   void inverse() {
      Matrix3 a = new Matrix3(1, 2, 3, 0, 6, 7, 0, 0, 12);
      JUnitUtils.assertEquals(Matrix3.IDENTITY, a.multiply(a.inverse()));
      a = new Matrix3(2, 9, 1, 11, 7, 2, 3, 16, 1);
      JUnitUtils.assertEquals(Matrix3.IDENTITY, a.multiply(a.inverse()), 1e-6f);
   }

   @Test
   void multiplyGetComponent() {
      Matrix3 a = new Matrix3(1, 2, 3, 4, 5, 6, 7, 8, 9);
      Vec3 b = new Vec3(17, 18, 19);
      assertEquals(a.multiply(b).x(), a.multiplyGetX(b));
      assertEquals(a.multiply(b).y(), a.multiplyGetY(b));
      assertEquals(a.multiply(b).z(), a.multiplyGetZ(b));
   }

   @Test
   void storeRowWise() {
      Matrix3 a = new Matrix3(1, 2, 3, 4, 5, 6, 7, 8, 9);
      ByteBuffer byteBuffer = ByteBuffer.allocate(4 * 9);
      a.writeRowWise(byteBuffer);
      byteBuffer.flip();
      Matrix3 b = Matrix3.readRowWise(byteBuffer);
      assertEquals(a, b);
   }

   @Test
   void determinant() {
      assertEquals(1, Matrix3.IDENTITY.determinant());
      Matrix3 a = new Matrix3(
            1, 6, 4,
            7, 5, 2,
            3, 8, 9);
      assertEquals(-149, a.determinant());
   }
}
