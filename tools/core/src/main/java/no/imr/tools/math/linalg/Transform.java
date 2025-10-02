package no.imr.tools.math.linalg;

@FunctionalInterface
public interface Transform {
   Vec3 transformPoint(Vec3 point);

   static Transform identity() {
      return point -> point;
   }
}
