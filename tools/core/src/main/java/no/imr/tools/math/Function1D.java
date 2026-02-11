package no.imr.tools.math;

import java.awt.geom.Point2D;

@FunctionalInterface
public interface Function1D {
   double eval(double x);

   static Function1D identity() {
      return x -> x;
   }

   static Function1D constant(double y) {
      return _ -> y;
   }

   static Function1D linear(double x0, double y0, double dyDx) {
      return x -> y0 + (x - x0) * dyDx;
   }

   static Function1D linear(Point2D pointA, Point2D pointB) {
      double x0 = pointA.getX();
      double y0 = pointA.getY();
      double dyDx = (pointB.getY() - y0) / (pointB.getX() - x0);
      return linear(x0, y0, dyDx);
   }

   static Function1D interpolate(double[] xValues, double[] yValues) {
      if (xValues.length != yValues.length) {
         throw new IllegalArgumentException(xValues.length + " != " + yValues.length);
      }
      if (xValues.length == 0) {
         throw new IllegalArgumentException("Empty arrays");
      }
      return x -> DoubleUtils.interpolate(xValues, yValues, x);
   }
}
