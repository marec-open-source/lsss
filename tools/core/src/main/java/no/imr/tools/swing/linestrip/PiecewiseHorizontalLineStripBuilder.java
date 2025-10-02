package no.imr.tools.swing.linestrip;

import no.marec.lsss.api.util.LineStripBuilder;

/**
 * Builds piecewise horizontal line strips.
 */
public final class PiecewiseHorizontalLineStripBuilder implements LineStripBuilder {
   private final LineStripBuilder lineStripBuilder;
   private boolean initialized;
   private int x0;
   private int y0;

   public PiecewiseHorizontalLineStripBuilder(LineStripBuilder lineStripBuilder) {
      this.lineStripBuilder = lineStripBuilder;
   }

   @Override
   public boolean isEmpty() {
      return lineStripBuilder.isEmpty();
   }

   private void addPoint(int x, int y) {
      if (!initialized) {
         initialized = true;
         lineStripBuilder.addPoint(x, y);
         x0 = x;
         y0 = y;
         return;
      }

      int dx = x - x0;
      if (dx > 1) {
         lineStripBuilder.addPoint(x - 1, y0);
      } else if (dx < -1) {
         lineStripBuilder.addPoint(x0 - 1, y);
      } else {
         // Next x coordinate maximum one pixel away => No need to draw horizontal line
      }

      lineStripBuilder.addPoint(x, y);

      x0 = x;
      y0 = y;
   }

   @Override
   public void addPoint(double x, double y) {
      addPoint((int) x, (int) y);
   }

   @Override
   public void endLineStrip() {
      initialized = false;
      lineStripBuilder.endLineStrip();
   }
}
