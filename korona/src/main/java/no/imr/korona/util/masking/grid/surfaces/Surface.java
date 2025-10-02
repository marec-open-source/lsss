package no.imr.korona.util.masking.grid.surfaces;

import java.awt.geom.Rectangle2D;

public abstract class Surface {
   protected Surface() {
   }

   public abstract Rectangle2D getBoundingBox();

   public abstract boolean contains(double x, double y);

   public abstract double z(double x, double y);
}
