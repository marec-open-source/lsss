package no.imr.tools.math.linalg;

import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class TRSTest {
   @Test
   void inverse() {
      TRS trs = new TRS(new Vec3(1, 2, 3), Matrix3.createRotation(17, new Vec3(1, 0, 1).unit()), 3);
      TRS inv = trs.inverse();
      check(TRS.IDENTITY, trs.multiply(inv));
      check(TRS.IDENTITY, inv.multiply(trs));
      check(TRS.IDENTITY, trs.divide(trs));
   }

   private static void check(TRS expected, TRS actual) {
      JUnitUtils.assertEquals(expected.translation(), actual.translation(), 1e-6f);
      JUnitUtils.assertEquals(expected.rotation(), actual.rotation(), 1e-6f);
      assertEquals(expected.scaling(), actual.scaling(), 1e-6f);
   }

   @Test
   void toMatrix4() {
      TRS trs = new TRS(new Vec3(1, 2, 3), Matrix3.createRotation(17, new Vec3(1, 0, 1).unit()), 3);
      Matrix4 matrix4 = trs.toMatrix4();
      Vec3 p = new Vec3(7, 1, 6);
      assertEquals(trs.transformPoint(p), matrix4.multiplyPos(p));
   }

   @Test
   void rotate() {
      TRS trs = new TRS(new Vec3(1, 2, 3), Matrix3.createRotation(17, new Vec3(1, 0, 1).unit()), 3);
      Matrix3 rotation = Matrix3.createRotation(-54, new Vec3(9, 6, -1).unit());
      Vec3 center = new Vec3(4, 5, 6);
      TRS trsRotated = trs.rotate(rotation, center);
      TRS trsRotatedBack = trsRotated.rotate(rotation.transpose(), center);
      check(trs, trsRotatedBack);
   }

   @Test
   void scale() {
      TRS trs = new TRS(new Vec3(1, 2, 3), Matrix3.createRotation(17, new Vec3(1, 0, 1).unit()), 3);
      float scale = 23;
      Vec3 center = new Vec3(4, 5, 6);
      TRS trsScaled = trs.scale(scale, center);
      TRS trsScaledBack = trsScaled.scale(1 / scale, center);
      check(trs, trsScaledBack);
   }

   @Test
   void untransformPoint() {
      TRS trs = new TRS(new Vec3(1, 2, 3), Matrix3.createRotation(17, new Vec3(1, 0, 1).unit()), 3);
      Vec3 p = new Vec3(7, 1, 6);
      Vec3 transformed = trs.transformPoint(p);
      Vec3 untransformed = trs.untransformPoint(transformed);
      JUnitUtils.assertEquals(p, untransformed, 1e-6f);
   }

   @Test
   void multiply() {
      Matrix3 rotA = Matrix3.createRotation(17, new Vec3(1, 0, 1).unit());
      Matrix3 rotB = Matrix3.createRotation(-54, new Vec3(9, 6, -1).unit());

      TRS a = new TRS(Vec3.ZERO, rotA, 1);
      TRS b = new TRS(Vec3.ZERO, rotB, 1);
      TRS prod = a.multiply(b);

      Vec3 v = new Vec3(7, 2, 3);
      Vec3 p = prod.transformPoint(v);
      Vec3 q = a.transformPoint(b.transformPoint(v));

      JUnitUtils.assertEquals(p, q, 1e-6f);
   }

   @Test
   void transformPointGetXYZ() {
      TRS trs = new TRS(new Vec3(1, 2, 3), Matrix3.createRotation(17, new Vec3(1, 0, 1).unit()), 3);
      Vec3 p = new Vec3(3, 2, 1);
      assertEquals(trs.transformPoint(p).x(), trs.transformPointGetX(p), 1e-6f);
      assertEquals(trs.transformPoint(p).y(), trs.transformPointGetY(p), 1e-6f);
      assertEquals(trs.transformPoint(p).z(), trs.transformPointGetZ(p), 1e-6f);
   }
}
