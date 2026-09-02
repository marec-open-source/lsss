package no.imr.tools.math.fit;

import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class IterativeFitTest {
   @Test
   void polynomialFit() {
      double a = 1.0;
      double b = 1.0;
      double c = 1.0;
      double d = 1.0;

      ThirdDegreePolynomial thirdDegreePolynomial = new ThirdDegreePolynomial(a, b, c, d);
      List<Point2D.Double> data = new ArrayList<>();
      for (int i = 0; i < 100; i++) {
         double x = i / 10.0;
         data.add(new Point2D.Double(x, thirdDegreePolynomial.evaluate(x)));
      }

      thirdDegreePolynomial.a.setValue(a + 0.6);
      thirdDegreePolynomial.b.setValue(b - 0.5);
      thirdDegreePolynomial.c.setValue(c - 0.4);
      thirdDegreePolynomial.d.setValue(d + 0.3);

      IterativeFit<Point2D.Double> iterativeFit = new IterativeFit<>(thirdDegreePolynomial, data);
      iterativeFit.doFit();

      assertEquals(a, thirdDegreePolynomial.a.getValue(), a * 0.2);
      assertEquals(b, thirdDegreePolynomial.b.getValue(), b * 0.2);
      assertEquals(c, thirdDegreePolynomial.c.getValue(), c * 0.2);
      assertEquals(d, thirdDegreePolynomial.d.getValue(), d * 0.2);
   }

   /**
    * Function of type f(x) = ax^3 - bx^2 - cx + d.
    */
   private static final class ThirdDegreePolynomial implements FitFunction<Point2D.Double> {
      private final FitParameter a = new FitParameter(1, 0.1);
      private final FitParameter b = new FitParameter(1, 0.1);
      private final FitParameter c = new FitParameter(1, 0.1);
      private final FitParameter d = new FitParameter(1, 0.1);

      private final List<FitParameter> parameters = List.of(a, b, c, d);

      private ThirdDegreePolynomial(double aPar, double bPar, double cPar, double dPar) {
         a.setValue(aPar);
         b.setValue(bPar);
         c.setValue(cPar);
         d.setValue(dPar);
      }

      @Override
      public double fittedValue(Point2D.Double dataPoint) {
         return evaluate(dataPoint.x);
      }

      @Override
      public double actualValue(Point2D.Double dataPoint) {
         return dataPoint.y;
      }

      private double evaluate(double x) {
         return a.getValue() * x * x * x
               - b.getValue() * x * x
               - c.getValue() * x
               + d.getValue();
      }

      @Override
      public List<FitParameter> getParameters() {
         return parameters;
      }
   }
}
