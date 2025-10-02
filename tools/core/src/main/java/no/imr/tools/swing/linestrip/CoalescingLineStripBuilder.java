package no.imr.tools.swing.linestrip;

import no.marec.lsss.api.util.LineStripBuilder;

/**
 * Coalesces subsequent line segments with equal slope.
 */
public final class CoalescingLineStripBuilder implements LineStripBuilder {
   private final LineStripBuilder lineStripBuilder;

   private boolean hasPoint;

   private int x0;
   private int y0;

   private boolean hasDirection;

   private int dx;
   private int dy;

   private double tMin;
   private double tMax;
   private double tLast;

   public CoalescingLineStripBuilder(LineStripBuilder lineStripBuilder) {
      this.lineStripBuilder = lineStripBuilder;
   }

   @Override
   public boolean isEmpty() {
      return lineStripBuilder.isEmpty();
   }

   private void addPoint(int x, int y) {
      if (!hasPoint) {
         lineStripBuilder.addPoint(x, y);
         hasPoint = true;
         x0 = x;
         y0 = y;
         return;
      }

      if (!hasDirection) {
         initDirection(x, y);
         hasDirection = dx != 0 || dy != 0;
         return;
      }

      if (dx * (y - y0) == dy * (x - x0)) {
         // Same line
         double t = dx != 0 ? (x - x0) / (double) dx : (y - y0) / (double) dy;
         if (t < tMin) {
            tMin = t;
         } else if (t > tMax) {
            tMax = t;
         }
         tLast = t;
         return;
      }

      addCurrentLine();

      x0 += (int) Math.round(tLast * dx);
      y0 += (int) Math.round(tLast * dy);
      initDirection(x, y);
   }

   @Override
   public void addPoint(double x, double y) {
      addPoint((int) x, (int) y);
   }

   @Override
   public void endLineStrip() {
      if (hasDirection) {
         addCurrentLine();
      }
      hasPoint = false;
      hasDirection = false;
      lineStripBuilder.endLineStrip();
   }

   private void initDirection(int x, int y) {
      dx = x - x0;
      dy = y - y0;
      tMin = 0;
      tMax = 1;
      tLast = 1;
   }

   private void addCurrentLine() {
      if (tLast == tMax) {
         if (tMin < 0) {
            addCurrentPoint(tMin);
         }
         addCurrentPoint(tLast);
      } else {
         addCurrentPoint(tMax);
         if (tMin < 0 && tMin != tLast) {
            addCurrentPoint(tMin);
         }
         addCurrentPoint(tLast);
      }
   }

   private void addCurrentPoint(double t) {
      lineStripBuilder.addPoint(x0 + t * dx, y0 + t * dy);
   }
}
