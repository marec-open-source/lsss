package no.imr.tools.math.linalg;

import no.imr.tools.Utils;

import java.awt.geom.Point2D;

/**
 * An immutable class representing a 2-dimensional vector.
 */
public record Vec2(float x, float y) {

   public Vec2(Point2D point) {
      this((float) point.getX(), (float) point.getY());
   }

   @Override
   public String toString() {
      return "[" + x + ", " + y + "]";
   }

   public Point2D.Float toPoint() {
      return new Point2D.Float(x, y);
   }

   public Vec3 toVec3() {
      return new Vec3(x, y, 0);
   }

   public Vec2 plus(float dx, float dy) {
      return new Vec2(x + dx, y + dy);
   }

   public Vec2 plus(Vec2 vec) {
      return new Vec2(x + vec.x, y + vec.y);
   }

   public Vec2 minus(float dx, float dy) {
      return new Vec2(x - dx, y - dy);
   }

   public Vec2 minus(Vec2 vec) {
      return new Vec2(x - vec.x, y - vec.y);
   }

   public float dot(Vec2 vec) {
      return x * vec.x + y * vec.y;
   }

   public float cross(Vec2 vec) {
      return x * vec.y - vec.x * y;
   }

   public Vec2 times(float f) {
      return new Vec2(x * f, y * f);
   }

   public Vec2 times(float fx, float fy) {
      return new Vec2(x * fx, y * fy);
   }

   public Vec2 times(Vec2 vec) {
      return new Vec2(x * vec.x, y * vec.y);
   }

   public Vec2 div(float f) {
      return new Vec2(x / f, y / f);
   }

   public Vec2 div(float fx, float fy) {
      return new Vec2(x / fx, y / fy);
   }

   public Vec2 div(Vec2 vec) {
      return new Vec2(x / vec.x, y / vec.y);
   }

   public Vec2 unit() {
      float length = length();
      return length == 0 ? this : div(length);
   }

   public float length() {
      return Utils.hypot(x, y);
   }
}
