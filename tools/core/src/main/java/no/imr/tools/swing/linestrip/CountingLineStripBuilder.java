package no.imr.tools.swing.linestrip;

import no.marec.lsss.api.util.LineStripBuilder;

/**
 * Coalesces subsequent line segments with equal slope.
 */
public final class CountingLineStripBuilder implements LineStripBuilder {
   private final LineStripBuilder lineStripBuilder;
   private int pointCount;

   public CountingLineStripBuilder(LineStripBuilder lineStripBuilder) {
      this.lineStripBuilder = lineStripBuilder;
   }

   public int getPointCount() {
      return pointCount;
   }

   @Override
   public boolean isEmpty() {
      return lineStripBuilder.isEmpty();
   }

   @Override
   public void addPoint(double x, double y) {
      lineStripBuilder.addPoint(x, y);
      pointCount++;
   }

   @Override
   public void endLineStrip() {
      lineStripBuilder.endLineStrip();
   }
}
