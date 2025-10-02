package no.imr.tools.math;

import no.imr.tools.math.linalg.Vec3;

@FunctionalInterface
public interface Function2D {
   double eval(double x, double y);

   static Function2D constant(double z) {
      return (x, y) -> z;
   }

   static Function2D linear(double x0, double y0, double z0, double dzDx, double dzDy) {
      return (x, y) -> z0 + (x - x0) * dzDx + (y - y0) * dzDy;
   }

   static Function2D linear(Vec3 p, double dzDx, double dzDy) {
      return linear(p.x(), p.y(), p.z(), dzDx, dzDy);
   }

   static Function2D sphere(double x0, double y0, double z0, double radius, boolean up) {
      double radius2 = radius * radius;
      return (x, y) -> {
         double dx = x - x0;
         double dy = y - y0;
         double dz = Math.sqrt(radius2 - dx * dx - dy * dy);
         return up ? z0 + dz : z0 - dz;
      };
   }

   static Function2D sphere(Vec3 center, double radius, boolean up) {
      return sphere(center.x(), center.y(), center.z(), radius, up);
   }
}
