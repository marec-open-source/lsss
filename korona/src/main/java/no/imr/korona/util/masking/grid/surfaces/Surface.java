package no.imr.korona.util.masking.grid.surfaces;

import java.awt.geom.Rectangle2D;

public interface Surface {
   Rectangle2D getBoundingBox();

   boolean contains(double x, double y);

   double z(double x, double y);
}
