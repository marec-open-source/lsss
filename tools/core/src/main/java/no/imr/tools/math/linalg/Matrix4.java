package no.imr.tools.math.linalg;

import java.nio.ByteBuffer;

/**
 * A 4x4 matrix.
 */
public record Matrix4(float m00, float m01, float m02, float m03,
                      float m10, float m11, float m12, float m13,
                      float m20, float m21, float m22, float m23,
                      float m30, float m31, float m32, float m33) {

   public static final Matrix4 IDENTITY = new Matrix4(
         1, 0, 0, 0,
         0, 1, 0, 0,
         0, 0, 1, 0,
         0, 0, 0, 1);

   public static Matrix4 ofColumns(Vec3 a, Vec3 b, Vec3 c) {
      return new Matrix4(
            a.x(), b.x(), c.x(), 0,
            a.y(), b.y(), c.y(), 0,
            a.z(), b.z(), c.z(), 0,
            0, 0, 0, 1);
   }

   @Override
   public String toString() {
      return "[ " +
            '[' + m00 + ", " + m01 + ", " + m02 + ", " + m03 + "], " +
            '[' + m10 + ", " + m11 + ", " + m12 + ", " + m13 + "], " +
            '[' + m20 + ", " + m21 + ", " + m22 + ", " + m23 + "], " +
            '[' + m30 + ", " + m31 + ", " + m32 + ", " + m33 + "] ]";
   }

   /**
    * Creates a new rotation matrix.
    *
    * @param angle        the rotation angle in degrees
    * @param rotationAxis the axis of rotation
    * @return a rotation matrix
    */
   public static Matrix4 createRotation(double angle, Vec3 rotationAxis) {
      float x = rotationAxis.x();
      float y = rotationAxis.y();
      float z = rotationAxis.z();
      double radians = Math.toRadians(angle);
      float c = (float) Math.cos(radians);
      float s = (float) Math.sin(radians);
      return new Matrix4(
            x * x * (1 - c) + c, x * y * (1 - c) - z * s, x * z * (1 - c) + y * s, 0,
            y * x * (1 - c) + z * s, y * y * (1 - c) + c, y * z * (1 - c) - x * s, 0,
            z * x * (1 - c) - y * s, z * y * (1 - c) + x * s, z * z * (1 - c) + c, 0,
            0, 0, 0, 1);
   }

   /**
    * Create a matrix for rotation so that a given vector will be aligned with a new
    * given direction. The rotation axis is the axis created by the cross product of
    * the original and the new vector.
    *
    * @param originalVector the original vector
    * @param newVector      the new vector orientation after applying the rotation matrix
    * @return the created matrix
    */
   public static Matrix4 createRotation(Vec3 originalVector, Vec3 newVector) {
      Vec3 newAxisUnit = newVector.unit();
      Vec3 origAxisUnit = originalVector.unit();
      float dot = origAxisUnit.dot(newAxisUnit);
      Vec3 cross = origAxisUnit.cross(newAxisUnit);
      // Since cross.length() is always non-negative, the angle will be between 0 and 180 degrees.
      double angle = Math.toDegrees(Math.atan2(cross.length(), dot));
      if (angle > 1e-4) {
         return createRotation(angle, cross.unit());
      }
      return IDENTITY;
   }

   /**
    * Creates a new translation matrix.
    *
    * @param translation the translation
    * @return a translation matrix
    */
   public static Matrix4 createTranslation(Vec3 translation) {
      return new Matrix4(
            1, 0, 0, translation.x(),
            0, 1, 0, translation.y(),
            0, 0, 1, translation.z(),
            0, 0, 0, 1);
   }

   /**
    * Creates a new scaling matrix.
    *
    * @param scaling the scaling
    * @return a translation matrix
    */
   public static Matrix4 createScaling(Vec3 scaling) {
      return new Matrix4(
            scaling.x(), 0, 0, 0,
            0, scaling.y(), 0, 0,
            0, 0, scaling.z(), 0,
            0, 0, 0, 1);
   }

   /**
    * Subtracts two matrices.
    *
    * @param B a matrix
    * @return this matrix - B
    */
   public Matrix4 minus(Matrix4 B) {
      return new Matrix4(
            m00 - B.m00, m01 - B.m01, m02 - B.m02, m03 - B.m03,
            m10 - B.m10, m11 - B.m11, m12 - B.m12, m13 - B.m13,
            m20 - B.m20, m21 - B.m21, m22 - B.m22, m23 - B.m23,
            m30 - B.m30, m31 - B.m31, m32 - B.m32, m33 - B.m33);
   }

   /**
    * Multiplies two matrices.
    *
    * @param B another matrix
    * @return this matrix * B
    */
   public Matrix4 multiply(Matrix4 B) {
      return new Matrix4(
            m00 * B.m00 + m01 * B.m10 + m02 * B.m20 + m03 * B.m30,
            m00 * B.m01 + m01 * B.m11 + m02 * B.m21 + m03 * B.m31,
            m00 * B.m02 + m01 * B.m12 + m02 * B.m22 + m03 * B.m32,
            m00 * B.m03 + m01 * B.m13 + m02 * B.m23 + m03 * B.m33,

            m10 * B.m00 + m11 * B.m10 + m12 * B.m20 + m13 * B.m30,
            m10 * B.m01 + m11 * B.m11 + m12 * B.m21 + m13 * B.m31,
            m10 * B.m02 + m11 * B.m12 + m12 * B.m22 + m13 * B.m32,
            m10 * B.m03 + m11 * B.m13 + m12 * B.m23 + m13 * B.m33,

            m20 * B.m00 + m21 * B.m10 + m22 * B.m20 + m23 * B.m30,
            m20 * B.m01 + m21 * B.m11 + m22 * B.m21 + m23 * B.m31,
            m20 * B.m02 + m21 * B.m12 + m22 * B.m22 + m23 * B.m32,
            m20 * B.m03 + m21 * B.m13 + m22 * B.m23 + m23 * B.m33,

            m30 * B.m00 + m31 * B.m10 + m32 * B.m20 + m33 * B.m30,
            m30 * B.m01 + m31 * B.m11 + m32 * B.m21 + m33 * B.m31,
            m30 * B.m02 + m31 * B.m12 + m32 * B.m22 + m33 * B.m32,
            m30 * B.m03 + m31 * B.m13 + m32 * B.m23 + m33 * B.m33);
   }

   public Vec3 multiplyVec(Vec2 v) {
      return new Vec3(m00 * v.x() + m01 * v.y(), m10 * v.x() + m11 * v.y(), m20 * v.x() + m21 * v.y());
   }

   /**
    * Multiplies this matrix with a vector.
    *
    * @param v a vector
    * @return the vector A*v
    */
   public Vec3 multiplyVec(Vec3 v) {
      return new Vec3(multiplyVecGetX(v), multiplyVecGetY(v), multiplyVecGetZ(v));
   }

   public float multiplyVecGetX(Vec3 v) {
      return m00 * v.x() + m01 * v.y() + m02 * v.z();
   }

   public float multiplyVecGetY(Vec3 v) {
      return m10 * v.x() + m11 * v.y() + m12 * v.z();
   }

   public float multiplyVecGetZ(Vec3 v) {
      return m20 * v.x() + m21 * v.y() + m22 * v.z();
   }

   /**
    * Multiplies this matrix with a position.
    *
    * @param v a vector
    * @return the vector A*v + t
    */
   public Vec3 multiplyPos(Vec3 v) {
      return new Vec3(multiplyPosGetX(v), multiplyPosGetY(v), multiplyPosGetZ(v));
   }

   public float multiplyPosGetX(Vec3 v) {
      return multiplyVecGetX(v) + m03;
   }

   public float multiplyPosGetY(Vec3 v) {
      return multiplyVecGetY(v) + m13;
   }

   public float multiplyPosGetZ(Vec3 v) {
      return multiplyVecGetZ(v) + m23;
   }

   public Matrix4 transpose() {
      return new Matrix4(
            m00, m10, m20, m30,
            m01, m11, m21, m31,
            m02, m12, m22, m32,
            m03, m13, m23, m33);
   }

   /**
    * Performs Gram-Schmidt orthonormalization.
    *
    * @return the orthonormalization of this matrix
    */
   public Matrix4 normalize() {
      Vec3 u = new Vec3(m00, m10, m20);
      Vec3 v = new Vec3(m01, m11, m21);

      u = u.unit();

      v = v.minus(u.times(u.dot(v))).unit();

      return ofColumns(u, v, u.cross(v));
   }

   public float[] toRowWiseArray() {
      return new float[]{
            m00, m01, m02, m03,
            m10, m11, m12, m13,
            m20, m21, m22, m23,
            m30, m31, m32, m33};
   }

   public float[] toColumnWiseArray() {
      return new float[]{
            m00, m10, m20, m30,
            m01, m11, m21, m31,
            m02, m12, m22, m32,
            m03, m13, m23, m33};
   }

   public static Matrix4 readRowWise(ByteBuffer byteBuffer) {
      return new Matrix4(
            byteBuffer.getFloat(), byteBuffer.getFloat(), byteBuffer.getFloat(), byteBuffer.getFloat(),
            byteBuffer.getFloat(), byteBuffer.getFloat(), byteBuffer.getFloat(), byteBuffer.getFloat(),
            byteBuffer.getFloat(), byteBuffer.getFloat(), byteBuffer.getFloat(), byteBuffer.getFloat(),
            byteBuffer.getFloat(), byteBuffer.getFloat(), byteBuffer.getFloat(), byteBuffer.getFloat());
   }

   public void writeRowWise(ByteBuffer byteBuffer) {
      byteBuffer.asFloatBuffer().put(toRowWiseArray());
      byteBuffer.position(byteBuffer.position() + 16 * 4);
   }

   public Matrix4 inverse() {
      float c00 = /**/  det3x3(/*               */   /**/   /**/ m11, m12, m13,   /**/   /**/ m21, m22, m23,   /**/   /**/ m31, m32, m33);
      float c01 = /**/ -det3x3(/*               */   /**/   m10, /**/ m12, m13,   /**/   m20, /**/ m22, m23,   /**/   m30, /**/ m32, m33);
      float c02 = /**/  det3x3(/*               */   /**/   m10, m11, /**/ m13,   /**/   m20, m21, /**/ m23,   /**/   m30, m31, /**/ m33);
      float c03 = /**/ -det3x3(/*               */   /**/   m10, m11, m12, /**/   /**/   m20, m21, m22, /**/   /**/   m30, m31, m32 /**/);

      float c10 = /**/ -det3x3(/**/ m01, m02, m03,   /**/   /*               */   /**/   /**/ m21, m22, m23,   /**/   /**/ m31, m32, m33);
      float c11 = /**/  det3x3(m00, /**/ m02, m03,   /**/   /*               */   /**/   m20, /**/ m22, m23,   /**/   m30, /**/ m32, m33);
      float c12 = /**/ -det3x3(m00, m01, /**/ m03,   /**/   /*               */   /**/   m20, m21, /**/ m23,   /**/   m30, m31, /**/ m33);
      float c13 = /**/  det3x3(m00, m01, m02, /**/   /**/   /*               */   /**/   m20, m21, m22, /**/   /**/   m30, m31, m32 /**/);

      float c20 = /**/  det3x3(/**/ m01, m02, m03,   /**/   /**/ m11, m12, m13,   /**/   /*               */   /**/   /**/ m31, m32, m33);
      float c21 = /**/ -det3x3(m00, /**/ m02, m03,   /**/   m10, /**/ m12, m13,   /**/   /*               */   /**/   m30, /**/ m32, m33);
      float c22 = /**/  det3x3(m00, m01, /**/ m03,   /**/   m10, m11, /**/ m13,   /**/   /*               */   /**/   m30, m31, /**/ m33);
      float c23 = /**/ -det3x3(m00, m01, m02, /**/   /**/   m10, m11, m12, /**/   /**/   /*               */   /**/   m30, m31, m32 /**/);

      float c30 = /**/ -det3x3(/**/ m01, m02, m03,   /**/   /**/ m11, m12, m13,   /**/   /**/ m21, m22, m23    /**/   /*               */);
      float c31 = /**/  det3x3(m00, /**/ m02, m03,   /**/   m10, /**/ m12, m13,   /**/   m20, /**/ m22, m23    /**/   /*               */);
      float c32 = /**/ -det3x3(m00, m01, /**/ m03,   /**/   m10, m11, /**/ m13,   /**/   m20, m21, /**/ m23    /**/   /*               */);
      float c33 = /**/  det3x3(m00, m01, m02, /**/   /**/   m10, m11, m12, /**/   /**/   m20, m21, m22 /**/    /**/   /*               */);

      float det = m00 * c00 + m01 * c01 + m02 * c02 + m03 * c03;

      return new Matrix4(
            c00 / det, c10 / det, c20 / det, c30 / det,
            c01 / det, c11 / det, c21 / det, c31 / det,
            c02 / det, c12 / det, c22 / det, c32 / det,
            c03 / det, c13 / det, c23 / det, c33 / det);
   }

   private static float det3x3(float a00, float a01, float a02,
                               float a10, float a11, float a12,
                               float a20, float a21, float a22) {
      return a00 * (a11 * a22 - a12 * a21)
            - a01 * (a10 * a22 - a12 * a20)
            + a02 * (a10 * a21 - a11 * a20);
   }
}
