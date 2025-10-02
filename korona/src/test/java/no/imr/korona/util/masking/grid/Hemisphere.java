package no.imr.korona.util.masking.grid;

import no.imr.korona.util.masking.grid.surfaces.Surface;

import java.awt.geom.Rectangle2D;

final class Hemisphere extends Surface {
   private final double radius2;
   private final boolean up;
   private final Rectangle2D boundingBox;

   Hemisphere(double radius, boolean up) {
      radius2 = radius * radius;
      this.up = up;
      boundingBox = new Rectangle2D.Double(-radius, -radius, 2 * radius, 2 * radius);
   }

   @Override
   public Rectangle2D getBoundingBox() {
      return boundingBox;
   }

   @Override
   public boolean contains(double x, double y) {
      return x * x + y * y <= radius2;
   }

   @Override
   public double z(double x, double y) {
      double z = Math.sqrt(radius2 - x * x - y * y);
      return up ? z : -z;
   }
}
