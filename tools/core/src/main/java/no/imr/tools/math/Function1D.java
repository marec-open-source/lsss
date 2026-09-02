package no.imr.tools.math;

import java.awt.geom.Point2D;
import java.util.function.DoubleUnaryOperator;

public final class Function1D {
   private Function1D() {
   }

   public static DoubleUnaryOperator constant(double y) {
      return _ -> y;
   }

   public static DoubleUnaryOperator linear(double x0, double y0, double dyDx) {
      return x -> y0 + (x - x0) * dyDx;
   }

   public static DoubleUnaryOperator linear(Point2D pointA, Point2D pointB) {
      double x0 = pointA.getX();
      double y0 = pointA.getY();
      double dyDx = (pointB.getY() - y0) / (pointB.getX() - x0);
      return linear(x0, y0, dyDx);
   }

   public static DoubleUnaryOperator interpolate(double[] xValues, double[] yValues) {
      if (xValues.length != yValues.length) {
         throw new IllegalArgumentException(xValues.length + " != " + yValues.length);
      }
      if (xValues.length == 0) {
         throw new IllegalArgumentException("Empty arrays");
      }
      return x -> DoubleUtils.interpolate(xValues, yValues, x);
   }
}
