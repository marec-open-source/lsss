package no.imr.tools.swing.linestrip;

import no.imr.tools.Utils;
import no.marec.lsss.api.util.LineStripBuilder;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Point2D;

/**
 * For creating shifted line strips.
 */
public final class ShiftedLineStripBuilder implements LineStripBuilder {
   private final LineStripBuilder lineStripBuilder;
   private final double shift;

   private Point2D.@Nullable Double previousPoint;
   private Point2D.@Nullable Double currentPoint;

   /**
    * Creates a new ShiftedLineStripBuilder.
    *
    * @param lineStripBuilder a line strip builder
    * @param shift            the amount to shift
    */
   public ShiftedLineStripBuilder(LineStripBuilder lineStripBuilder, double shift) {
      this.lineStripBuilder = lineStripBuilder;
      this.shift = shift;
   }

   @Override
   public boolean isEmpty() {
      return lineStripBuilder.isEmpty();
   }

   @Override
   public void addPoint(double x, double y) {
      Point2D.Double nextPoint = new Point2D.Double(x, y);

      if (previousPoint == null) {
         // First point => Just save it.
         previousPoint = nextPoint;
         return;
      }

      if (currentPoint == null) {
         // Second point => Add the shifted first point.
         addShiftedPoint(previousPoint, previousPoint, nextPoint);
         currentPoint = nextPoint;
         return;
      }

      // Add the shifted current point.
      addShiftedPoint(previousPoint, currentPoint, nextPoint);
      previousPoint = currentPoint;
      currentPoint = nextPoint;
   }

   private void addShiftedPoint(Point2D.Double first, Point2D.Double point, Point2D.Double last) {
      double dx = last.x - first.x;
      double dy = last.y - first.y;
      double d = Utils.hypot(dx, dy);
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
      if (previousPoint != null && currentPoint != null) {
         // Add the shifted last point.
         addShiftedPoint(previousPoint, currentPoint, currentPoint);
      }
      previousPoint = null;
      currentPoint = null;
      lineStripBuilder.endLineStrip();
   }
}
