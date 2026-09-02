package no.imr.korona.util.masking.grid.surfaces;

import no.imr.tools.math.Function2D;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;

public final class ShapeLimitedSurface implements Surface {
   private final Shape shape;
   private final Function2D function;
   private final Rectangle2D boundingBox;

   public ShapeLimitedSurface(Shape shape, Function2D function) {
      this.shape = shape;
      this.function = function;
      boundingBox = shape.getBounds2D();
   }

   @Override
   public Rectangle2D getBoundingBox() {
      return boundingBox;
   }

   @Override
   public boolean contains(double x, double y) {
      return boundingBox.contains(x, y) && shape.contains(x, y);
   }

   @Override
   public double z(double x, double y) {
      return function.eval(x, y);
   }
}
