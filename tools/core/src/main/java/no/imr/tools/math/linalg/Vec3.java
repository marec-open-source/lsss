package no.imr.tools.math.linalg;

import java.awt.geom.Point2D;

/**
 * An immutable class representing a 3-dimensional vector.
 */
public record Vec3(float x, float y, float z) {

   public static final Vec3 ZERO = new Vec3(0, 0, 0);

   public Vec3(Point2D point) {
      this((float) point.getX(), (float) point.getY(), 0);
   }

   @Override
   public String toString() {
      return "[" + x + ", " + y + ", " + z + "]";
   }

   public Point2D.Float toPoint() {
      return new Point2D.Float(x, y);
   }

   public Vec2 toVec2() {
      return new Vec2(x, y);
   }

   public Vec3 plus(float dx, float dy, float dz) {
      return new Vec3(x + dx, y + dy, z + dz);
   }

   public Vec3 plus(Vec3 vec) {
      return new Vec3(x + vec.x, y + vec.y, z + vec.z);
   }

   public Vec3 minus(float dx, float dy, float dz) {
      return new Vec3(x - dx, y - dy, z - dz);
   }

   public Vec3 minus(Vec3 vec) {
      return new Vec3(x - vec.x, y - vec.y, z - vec.z);
   }

   public float dot(Vec3 vec) {
      return x * vec.x + y * vec.y + z * vec.z;
   }

   public Vec3 cross(Vec3 vec) {
      return new Vec3(
            y * vec.z - vec.y * z,
            z * vec.x - vec.z * x,
            x * vec.y - vec.x * y);
   }

   public float crossGetZ(Vec3 vec) {
      return x * vec.y - vec.x * y;
   }

   public Vec3 times(float f) {
      return new Vec3(x * f, y * f, z * f);
   }

   public Vec3 times(float fx, float fy, float fz) {
      return new Vec3(x * fx, y * fy, z * fz);
   }

   public Vec3 times(Vec3 vec) {
      return new Vec3(x * vec.x, y * vec.y, z * vec.z);
   }

   public Vec3 div(float f) {
      return new Vec3(x / f, y / f, z / f);
   }

   public Vec3 div(float fx, float fy, float fz) {
      return new Vec3(x / fx, y / fy, z / fz);
   }

   public Vec3 div(Vec3 vec) {
      return new Vec3(x / vec.x, y / vec.y, z / vec.z);
   }

   public Vec3 unit() {
      float length = length();
      return length == 0 ? this : div(length);
   }

   public Vec3 abs() {
      return new Vec3(Math.abs(x), Math.abs(y), Math.abs(z));
   }

   public float length() {
      return (float) Math.sqrt(dot(this));
   }

   public Vec3 someOrthonormalVector() {
      float ax = Math.abs(x);
      float ay = Math.abs(y);
      float az = Math.abs(z);
      if (ax <= ay && ax <= az) {
         // ax is smallest.
         return new Vec3(0, -z, y).unit();
      } else if (ay <= az) {
         // ay is smallest.
         return new Vec3(-z, 0, x).unit();
      } else {
         // az is smallest.
         //noinspection SuspiciousNameCombination
         return new Vec3(-y, x, 0).unit();
      }
   }
}
