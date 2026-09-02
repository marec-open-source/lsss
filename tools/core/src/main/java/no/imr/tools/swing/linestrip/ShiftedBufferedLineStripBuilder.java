package no.imr.tools.swing.linestrip;

import no.imr.tools.math.MathUtils;
import no.marec.lsss.api.util.LineStripBuilder;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Like {@link ShiftedLineStripBuilder}, but uses more points to increase robustness.
 */
public final class ShiftedBufferedLineStripBuilder implements LineStripBuilder {
   private final LineStripBuilder lineStripBuilder;
   private final double shift;
   private final int radius;

   private final List<Point2D.Double> points = new ArrayList<>();
   private boolean initialized;

   public ShiftedBufferedLineStripBuilder(LineStripBuilder lineStripBuilder, double shift, int radius) {
      if (radius <= 0) {
         throw new IllegalArgumentException();
      }
      this.lineStripBuilder = lineStripBuilder;
      this.shift = shift;
      this.radius = radius;
   }

   @Override
   public boolean isEmpty() {
      return lineStripBuilder.isEmpty();
   }

   @Override
   public void addPoint(double x, double y) {
      Point2D.Double point = new Point2D.Double(x, y);

      if (initialized) {
         points.removeFirst();
         points.add(point);
         addShiftedPoint(points.get(radius));
      } else {
         points.add(point);
         if (points.size() == radius * 2 + 1) {
            for (int i = 0; i <= radius; i++) {
               addShiftedPoint(points.get(i));
            }
            initialized = true;
         }
      }
   }

   private void addShiftedPoint(Point2D.Double point) {
      Point2D.Double first = points.getFirst();
      Point2D.Double last = points.getLast();

      double dx = last.x - first.x;
      double dy = last.y - first.y;
      double d = MathUtils.hypot(dx, dy);
      if (d == 0) {
         return;
      }
      double f = shift / d;
      double x = point.x + dy * f;
      double y = point.y - dx * f;
      lineStripBuilder.addPoint(x, y);
   }

   @Override
   public void endLineStrip() {
      for (int i = initialized ? radius + 1 : 0; i < points.size(); i++) {
         addShiftedPoint(points.get(i));
      }
      points.clear();
      initialized = false;
      lineStripBuilder.endLineStrip();
   }
}
