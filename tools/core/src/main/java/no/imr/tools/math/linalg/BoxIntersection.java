package no.imr.tools.math.linalg;

import no.imr.tools.math.MathUtils;

public final class BoxIntersection {
   private BoxIntersection() {
   }

   public static boolean boxIntersectsQuad(Vec3 boxCorner, Vec3 boxSize, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4) {
      return unitBoxIntersectsQuad(
            p1.minus(boxCorner).div(boxSize),
            p2.minus(boxCorner).div(boxSize),
            p3.minus(boxCorner).div(boxSize),
            p4.minus(boxCorner).div(boxSize)
      );
   }

   public static boolean unitBoxIntersectsQuad(Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4) {
      return unitBoxIntersectsTriangle(p1, p2, p3) || unitBoxIntersectsTriangle(p3, p4, p1);
   }

   public static boolean boxIntersectsTriangle(Vec3 boxOrigin, Vec3 boxSize, Vec3 p1, Vec3 p2, Vec3 p3) {
      return unitBoxIntersectsTriangle(
            p1.minus(boxOrigin).div(boxSize),
            p2.minus(boxOrigin).div(boxSize),
            p3.minus(boxOrigin).div(boxSize)
      );
   }

   public static boolean unitBoxIntersectsTriangle(Vec3 p1, Vec3 p2, Vec3 p3) {
      if (p1.x() < 0 && p2.x() < 0 && p3.x() < 0 ||
            p1.x() > 1 && p2.x() > 1 && p3.x() > 1 ||

            p1.y() < 0 && p2.y() < 0 && p3.y() < 0 ||
            p1.y() > 1 && p2.y() > 1 && p3.y() > 1 ||

            p1.z() < 0 && p2.z() < 0 && p3.z() < 0 ||
            p1.z() > 1 && p2.z() > 1 && p3.z() > 1) {
         return false;
      }

      if (unitBoxIntersectsPoint(p1) || unitBoxIntersectsPoint(p2) || unitBoxIntersectsPoint(p3)) {
         return true;
      }

      if (unitBoxIntersectsLine(p1, p2) || unitBoxIntersectsLine(p2, p3) || unitBoxIntersectsLine(p3, p1)) {
         return true;
      }

      Vec3 p1p2 = p2.minus(p1);
      Vec3 p1p3 = p3.minus(p1);
      return triangleIntersectsUnitLineX(p1, p1p2, p1p3, 0, 0) ||
            triangleIntersectsUnitLineX(p1, p1p2, p1p3, 1, 0) ||
            triangleIntersectsUnitLineX(p1, p1p2, p1p3, 1, 1) ||
            triangleIntersectsUnitLineX(p1, p1p2, p1p3, 0, 1) ||

            triangleIntersectsUnitLineY(p1, p1p2, p1p3, 0, 0) ||
            triangleIntersectsUnitLineY(p1, p1p2, p1p3, 1, 0) ||
            triangleIntersectsUnitLineY(p1, p1p2, p1p3, 1, 1) ||
            triangleIntersectsUnitLineY(p1, p1p2, p1p3, 0, 1) ||

            triangleIntersectsUnitLineZ(p1, p1p2, p1p3, 0, 0) ||
            triangleIntersectsUnitLineZ(p1, p1p2, p1p3, 1, 0) ||
            triangleIntersectsUnitLineZ(p1, p1p2, p1p3, 1, 1) ||
            triangleIntersectsUnitLineZ(p1, p1p2, p1p3, 0, 1);
   }

   public static boolean unitBoxIntersectsLine(Vec3 p1, Vec3 p2) {
      if (p1.x() < 0 && p2.x() < 0 ||
            p1.x() > 1 && p2.x() > 1 ||

            p1.y() < 0 && p2.y() < 0 ||
            p1.y() > 1 && p2.y() > 1 ||

            p1.z() < 0 && p2.z() < 0 ||
            p1.z() > 1 && p2.z() > 1) {
         return false;
      }

      return unitSquareIntersectsLine(0, /**/ p1.x(), p1.y(), p1.z(), /**/ p2.x(), p2.y(), p2.z()) ||
            unitSquareIntersectsLine(1, /**/ p1.x(), p1.y(), p1.z(), /**/ p2.x(), p2.y(), p2.z()) ||
            unitSquareIntersectsLine(0, /**/ p1.y(), p1.z(), p1.x(), /**/ p2.y(), p2.z(), p2.x()) ||
            unitSquareIntersectsLine(1, /**/ p1.y(), p1.z(), p1.x(), /**/ p2.y(), p2.z(), p2.x()) ||
            unitSquareIntersectsLine(0, /**/ p1.z(), p1.x(), p1.y(), /**/ p2.z(), p2.x(), p2.y()) ||
            unitSquareIntersectsLine(1, /**/ p1.z(), p1.x(), p1.y(), /**/ p2.z(), p2.x(), p2.y());
   }

   private static boolean unitSquareIntersectsLine(float z,
                                                   float p1x, float p1y, float p1z,
                                                   float p2x, float p2y, float p2z) {
      if (p1z < z == p2z < z) {
         return false;
      }
      float a = (z - p1z) / (p2z - p1z);
      float x = (float) MathUtils.interpolate(p1x, p2x, a);
      if (x < 0 || x > 1) {
         return false;
      }
      float y = (float) MathUtils.interpolate(p1y, p2y, a);
      return y >= 0 && y <= 1;
   }

   public static boolean unitBoxIntersectsPoint(Vec3 p) {
      return p.x() >= 0 && p.x() <= 1
            && p.y() >= 0 && p.y() <= 1
            && p.z() >= 0 && p.z() <= 1;
   }

   private static boolean triangleIntersectsUnitLineX(Vec3 tp, Vec3 tv1, Vec3 tv2, float y, float z) {
      float c02 = tv2.y() * tv1.z() - tv1.y() * tv2.z();
      float c12 = tv1.x() * tv2.z() - tv2.x() * tv1.z();
      float c22 = tv2.x() * tv1.y() - tv1.x() * tv2.y();

      float det = c02;

      float b0 = tp.x();
      float b1 = tp.y() - y;
      float b2 = tp.z() - z;

      float a = (tv2.z() * b1 - tv2.y() * b2) / det;
      float b = (tv1.y() * b2 - tv1.z() * b1) / det;
      float c = (c02 * b0 + c12 * b1 + c22 * b2) / det;

      return a >= 0 && b >= 0 && a + b <= 1 && c >= 0 && c <= 1;
   }

   private static boolean triangleIntersectsUnitLineY(Vec3 tp, Vec3 tv1, Vec3 tv2, float x, float z) {
      float c02 = tv2.y() * tv1.z() - tv1.y() * tv2.z();
      float c12 = tv1.x() * tv2.z() - tv2.x() * tv1.z();
      float c22 = tv2.x() * tv1.y() - tv1.x() * tv2.y();

      float det = c12;

      float b0 = tp.x() - x;
      float b1 = tp.y();
      float b2 = tp.z() - z;

      float a = (tv2.x() * b2 - tv2.z() * b0) / det;
      float b = (tv1.z() * b0 - tv1.x() * b2) / det;
      float c = (c02 * b0 + c12 * b1 + c22 * b2) / det;

      return a >= 0 && b >= 0 && a + b <= 1 && c >= 0 && c <= 1;
   }

   private static boolean triangleIntersectsUnitLineZ(Vec3 tp, Vec3 tv1, Vec3 tv2, float x, float y) {
      float c02 = tv2.y() * tv1.z() - tv1.y() * tv2.z();
      float c12 = tv1.x() * tv2.z() - tv2.x() * tv1.z();
      float c22 = tv2.x() * tv1.y() - tv1.x() * tv2.y();

      float det = c22;

      float b0 = tp.x() - x;
      float b1 = tp.y() - y;
      float b2 = tp.z();

      float a = (tv2.y() * b0 - tv2.x() * b1) / det;
      float b = (tv1.x() * b1 - tv1.y() * b0) / det;
      float c = (c02 * b0 + c12 * b1 + c22 * b2) / det;

      return a >= 0 && b >= 0 && a + b <= 1 && c >= 0 && c <= 1;
   }
}
