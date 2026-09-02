package no.imr.tools.math.linalg;

import java.nio.ByteBuffer;

/**
 * A 3x3 matrix.
 */
public record Matrix3(float m00, float m01, float m02,
                      float m10, float m11, float m12,
                      float m20, float m21, float m22) {

   public static final Matrix3 IDENTITY = new Matrix3(
         1, 0, 0,
         0, 1, 0,
         0, 0, 1);

   public static Matrix3 ofColumns(Vec3 a, Vec3 b, Vec3 c) {
      return new Matrix3(
            a.x(), b.x(), c.x(),
            a.y(), b.y(), c.y(),
            a.z(), b.z(), c.z());
   }

   @Override
   public String toString() {
      return "[ " +
            '[' + m00 + ", " + m01 + ", " + m02 + "], " +
            '[' + m10 + ", " + m11 + ", " + m12 + "], " +
            '[' + m20 + ", " + m21 + ", " + m22 + "] ]";
   }

   /**
    * Creates a new rotation matrix.
    *
    * @param angle        the rotation angle in degrees
    * @param rotationAxis the axis of rotation
    * @return a rotation matrix
    */
   public static Matrix3 createRotation(double angle, Vec3 rotationAxis) {
      float x = rotationAxis.x();
      float y = rotationAxis.y();
      float z = rotationAxis.z();
      double radians = Math.toRadians(angle);
      float c = (float) Math.cos(radians);
      float s = (float) Math.sin(radians);
      return new Matrix3(
            x * x * (1 - c) + c, x * y * (1 - c) - z * s, x * z * (1 - c) + y * s,
            y * x * (1 - c) + z * s, y * y * (1 - c) + c, y * z * (1 - c) - x * s,
            z * x * (1 - c) - y * s, z * y * (1 - c) + x * s, z * z * (1 - c) + c);
   }

   /**
    * Create a matrix for rotation so that a given vector will be aligned with a new
    * given direction. The rotation axis is the axis created by the cross product of
    * the original and the new vector.
    *
    * @param originalVector the original vector
    * @param newVector      the new vector orientation after applying the rotation matrix
    * @return a rotation matrix
    */
   public static Matrix3 createRotation(Vec3 originalVector, Vec3 newVector) {
      Vec3 newAxisUnit = newVector.unit();
      Vec3 origAxisUnit = originalVector.unit();
      float dot = origAxisUnit.dot(newAxisUnit);
      Vec3 cross = origAxisUnit.cross(newAxisUnit);
      // Since cross.length() is always non-negative, the angle will be between 0 and 180 degrees.
      float crossLength = cross.length();
      double angle = Math.toDegrees(Math.atan2(crossLength, dot));
      if (angle < 1e-4) {
         return IDENTITY;
      }
      Vec3 rotationAxis = crossLength == 0
            ? originalVector.someOrthonormalVector()
            : cross.div(crossLength);
      return createRotation(angle, rotationAxis);
   }

   /**
    * Creates a new scaling matrix.
    *
    * @param scaling the scaling
    * @return a scaling matrix
    */
   public static Matrix3 createScaling(Vec3 scaling) {
      return new Matrix3(
            scaling.x(), 0, 0,
            0, scaling.y(), 0,
            0, 0, scaling.z());
   }

   public Matrix4 toMatrix4() {
      return new Matrix4(
            m00, m01, m02, 0,
            m10, m11, m12, 0,
            m20, m21, m22, 0,
            0, 0, 0, 1);
   }

   public Matrix3 minus(Matrix3 b) {
      return new Matrix3(
            m00 - b.m00, m01 - b.m01, m02 - b.m02,
            m10 - b.m10, m11 - b.m11, m12 - b.m12,
            m20 - b.m20, m21 - b.m21, m22 - b.m22);
   }

   public Matrix3 multiply(Matrix3 b) {
      return new Matrix3(
            m00 * b.m00 + m01 * b.m10 + m02 * b.m20,
            m00 * b.m01 + m01 * b.m11 + m02 * b.m21,
            m00 * b.m02 + m01 * b.m12 + m02 * b.m22,

            m10 * b.m00 + m11 * b.m10 + m12 * b.m20,
            m10 * b.m01 + m11 * b.m11 + m12 * b.m21,
            m10 * b.m02 + m11 * b.m12 + m12 * b.m22,

            m20 * b.m00 + m21 * b.m10 + m22 * b.m20,
            m20 * b.m01 + m21 * b.m11 + m22 * b.m21,
            m20 * b.m02 + m21 * b.m12 + m22 * b.m22);
   }

   public Vec3 multiply(Vec2 v) {
      return new Vec3(m00 * v.x() + m01 * v.y(), m10 * v.x() + m11 * v.y(), m20 * v.x() + m21 * v.y());
   }

   public Vec3 multiply(Vec3 v) {
      return new Vec3(multiplyGetX(v), multiplyGetY(v), multiplyGetZ(v));
   }

   public float multiplyGetX(Vec3 v) {
      return m00 * v.x() + m01 * v.y() + m02 * v.z();
   }

   public float multiplyGetY(Vec3 v) {
      return m10 * v.x() + m11 * v.y() + m12 * v.z();
   }

   public float multiplyGetZ(Vec3 v) {
      return m20 * v.x() + m21 * v.y() + m22 * v.z();
   }

   public Matrix3 transpose() {
      return new Matrix3(
            m00, m10, m20,
            m01, m11, m21,
            m02, m12, m22);
   }

   /**
    * Performs Gram-Schmidt orthonormalization.
    *
    * @return the orthonormalization of this matrix
    */
   public Matrix3 normalize() {
      Vec3 u = new Vec3(m00, m10, m20);
      Vec3 v = new Vec3(m01, m11, m21);

      u = u.unit();

      v = v.minus(u.times(u.dot(v))).unit();

      return ofColumns(u, v, u.cross(v));
   }

   public float[] toRowWiseArray() {
      return new float[]{
            m00, m01, m02,
            m10, m11, m12,
            m20, m21, m22};
   }

   public float[] toColumnWiseArray() {
      return new float[]{
            m00, m10, m20,
            m01, m11, m21,
            m02, m12, m22};
   }

   public static Matrix3 readRowWise(ByteBuffer byteBuffer) {
      return new Matrix3(
            byteBuffer.getFloat(), byteBuffer.getFloat(), byteBuffer.getFloat(),
            byteBuffer.getFloat(), byteBuffer.getFloat(), byteBuffer.getFloat(),
            byteBuffer.getFloat(), byteBuffer.getFloat(), byteBuffer.getFloat());
   }

   public void writeRowWise(ByteBuffer byteBuffer) {
      byteBuffer.asFloatBuffer().put(toRowWiseArray());
      byteBuffer.position(byteBuffer.position() + 9 * 4);
   }

   public Matrix3 inverse() {
      float c00 = m11 * m22 - m12 * m21;
      float c01 = m12 * m20 - m10 * m22;
      float c02 = m10 * m21 - m11 * m20;

      float c10 = m02 * m21 - m01 * m22;
      float c11 = m00 * m22 - m02 * m20;
      float c12 = m01 * m20 - m00 * m21;

      float c20 = m01 * m12 - m02 * m11;
      float c21 = m02 * m10 - m00 * m12;
      float c22 = m00 * m11 - m01 * m10;

      float det = m00 * c00 + m01 * c01 + m02 * c02;

      return new Matrix3(
            c00 / det, c10 / det, c20 / det,
            c01 / det, c11 / det, c21 / det,
            c02 / det, c12 / det, c22 / det);
   }

   public float determinant() {
      return m00 * (m11 * m22 - m12 * m21)
            - m01 * (m10 * m22 - m12 * m20)
            + m02 * (m10 * m21 - m11 * m20);
   }
}
