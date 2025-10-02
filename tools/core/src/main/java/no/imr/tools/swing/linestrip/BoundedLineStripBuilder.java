package no.imr.tools.swing.linestrip;

import no.marec.lsss.api.util.LineStripBuilder;

import java.awt.geom.Rectangle2D;

/**
 * Discards line segments outside a given bounding box.
 */
public final class BoundedLineStripBuilder implements LineStripBuilder {
   private final LineStripBuilder lineStripBuilder;
   private final Rectangle2D bounds;
   private boolean hasPoint;
   private boolean addedPoint;
   private double x0;
   private double y0;

   public BoundedLineStripBuilder(LineStripBuilder lineStripBuilder, Rectangle2D bounds) {
      this.lineStripBuilder = lineStripBuilder;
      this.bounds = bounds;
   }

   @Override
   public boolean isEmpty() {
      return lineStripBuilder.isEmpty();
   }

   @Override
   public void addPoint(double x, double y) {
      if (!hasPoint) {
         hasPoint = true;
         addedPoint = false;
         x0 = x;
         y0 = y;
         return;
      }

      if (bounds.intersectsLine(x0, y0, x, y)) {
         if (!addedPoint) {
            lineStripBuilder.addPoint(x0, y0);
         }
         lineStripBuilder.addPoint(x, y);
         addedPoint = true;
      } else {
         lineStripBuilder.endLineStrip();
         addedPoint = false;
      }
      x0 = x;
      y0 = y;
   }

   @Override
   public void endLineStrip() {
      hasPoint = false;
      addedPoint = false;
      lineStripBuilder.endLineStrip();
   }
}
