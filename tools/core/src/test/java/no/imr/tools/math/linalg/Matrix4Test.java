package no.imr.tools.math.linalg;

import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

final class Matrix4Test {
   @Test
   void createRotationFromAngleAndAxis() {
      Vec3 axis = new Vec3(1, 2, 3);
      float angle = 53;
      JUnitUtils.assertEquals(Matrix3.createRotation(angle, axis).toMatrix4(), Matrix4.createRotation(angle, axis));
      JUnitUtils.assertEquals(Matrix3.createRotation(-angle, axis).toMatrix4(), Matrix4.createRotation(-angle, axis));
   }

   @Test
   void createRotationFromTwoDirections() {
      JUnitUtils.assertEquals(
            Matrix3.createRotation(new Vec3(1, 0, 0), new Vec3(1, 0, 1e-9f)).toMatrix4(),
            Matrix4.createRotation(new Vec3(1, 0, 0), new Vec3(1, 0, 1e-9f))
      );
      JUnitUtils.assertEquals(
            Matrix3.createRotation(new Vec3(1, 0, 0), new Vec3(0, 1, 0)).toMatrix4(),
            Matrix4.createRotation(new Vec3(1, 0, 0), new Vec3(0, 1, 0))
      );
      JUnitUtils.assertEquals(
            Matrix3.createRotation(new Vec3(1, 1, 1), new Vec3(-1, -1, -1)).toMatrix4(),
            Matrix4.createRotation(new Vec3(1, 1, 1), new Vec3(-1, -1, -1))
      );
   }

   @Test
   void createTranslation() {
      Matrix4 a = Matrix4.createTranslation(new Vec3(1, 2, 3));
      Matrix4 b = Matrix4.createTranslation(new Vec3(-1, -2, -3));
      Matrix4 c = a.multiply(b);
      JUnitUtils.assertEquals(Matrix4.IDENTITY, c);
   }

   @Test
   void createScaling() {
      Matrix4 a = Matrix4.createScaling(new Vec3(1, 2, 4));
      Matrix4 b = Matrix4.createScaling(new Vec3(1, 0.5f, 0.25f));
      Matrix4 c = a.multiply(b);
      JUnitUtils.assertEquals(Matrix4.IDENTITY, c);
   }

   @Test
   void multiplyVec() {
      Matrix4 m = new Matrix4(
            1, 2, 3, 4,
            5, 0, 0, 0,
            6, 0, 0, 0,
            7, 0, 0, 0);

      Vec3 res = m.multiplyVec(new Vec3(1, 1, 1));
      JUnitUtils.assertEquals(new Vec3(6, 5, 6), res);
   }

   @Test
   void minus() {
      Matrix4 a = new Matrix4(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
      JUnitUtils.assertEquals(new Matrix4(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), a.minus(a));
      JUnitUtils.assertEquals(a, a.minus(new Matrix4(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
   }

   @Test
   void transpose() {
      Matrix4 m = new Matrix4(
            1, 2, 3, 4,
            5, 6, 7, 8,
            9, 10, 11, 12,
            13, 14, 15, 16);
      Matrix4 t = new Matrix4(
            1, 5, 9, 13,
            2, 6, 10, 14,
            3, 7, 11, 15,
            4, 8, 12, 16);
      JUnitUtils.assertEquals(t, m.transpose());
   }

   @Test
   void inverse() {
      Matrix4 a = new Matrix4(1, 2, 3, 4, 0, 6, 7, 8, 0, 0, 11, 12, 0, 0, 0, 16);
      JUnitUtils.assertEquals(Matrix4.IDENTITY, a.multiply(a.inverse()));
      a = new Matrix4(2, 9, 1, 7, 9, 6, 7, 6, 4, 2, 11, 7, 11, 3, 16, 1);
      JUnitUtils.assertEquals(Matrix4.IDENTITY, a.multiply(a.inverse()), 1e-6f);
   }

   @Test
   void multiplyPosGetComponent() {
      Matrix4 a = new Matrix4(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
      Vec3 b = new Vec3(17, 18, 19);
      assertEquals(a.multiplyPos(b).x(), a.multiplyPosGetX(b));
      assertEquals(a.multiplyPos(b).y(), a.multiplyPosGetY(b));
      assertEquals(a.multiplyPos(b).z(), a.multiplyPosGetZ(b));
   }

   @Test
   void storeRowWise() {
      Matrix4 a = new Matrix4(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
      ByteBuffer byteBuffer = ByteBuffer.allocate(4 * 16);
      a.writeRowWise(byteBuffer);
      byteBuffer.flip();
      Matrix4 b = Matrix4.readRowWise(byteBuffer);
      assertEquals(a, b);
   }
}
