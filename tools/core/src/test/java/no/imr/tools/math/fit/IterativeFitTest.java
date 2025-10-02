package no.imr.tools.math.fit;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class IterativeFitTest {
   @Test
   void polynomialFit() {
      double a = 1.0;
      double b = 1.0;
      double c = 1.0;
      double d = 1.0;

      ThirdDegreePolynomial thirdDegreePolynomial = new ThirdDegreePolynomial(a, b, c, d);
      Set<FitDataPoint> data = new HashSet<>();

      for (int i = 0; i < 100; i++) {
         List<Double> list = new ArrayList<>();
         list.add(i / 10.0);

         data.add(new XYDataPoint(list.getFirst(), thirdDegreePolynomial.evaluate(list)));
      }

      thirdDegreePolynomial.a.setValue(a + 0.6);
      thirdDegreePolynomial.b.setValue(b - 0.5);
      thirdDegreePolynomial.c.setValue(c - 0.4);
      thirdDegreePolynomial.d.setValue(d + 0.3);

      IterativeFit iterativeFit = new IterativeFit(thirdDegreePolynomial, data);
      iterativeFit.doFit();

      List<FitParameter> parameters = thirdDegreePolynomial.getParameters();

      assertEquals(a, parameters.get(0).getValue(), a * 0.2);
      assertEquals(b, parameters.get(1).getValue(), b * 0.2);
      assertEquals(c, parameters.get(2).getValue(), c * 0.2);
      assertEquals(d, parameters.get(3).getValue(), d * 0.2);
   }

   /**
    * Function of type f(x) = ax^3 - bx^2 - cx + d.
    */
   private static final class ThirdDegreePolynomial implements FitFunction {
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
      public double evaluate(List<Double> arg) {
         double x = arg.getFirst();
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

   private static final class XYDataPoint implements FitDataPoint {
      private final List<Double> arguments;
      private final double value;

      private XYDataPoint(double x, double y) {
         arguments = List.of(x);
         value = y;
      }

      @Override
      public List<Double> getArguments() {
         return arguments;
      }

      @Override
      public double getValue() {
         return value;
      }
   }
}
