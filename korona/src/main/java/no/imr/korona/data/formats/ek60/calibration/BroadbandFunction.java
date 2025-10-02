package no.imr.korona.data.formats.ek60.calibration;

import no.imr.tools.Utils;
import no.imr.tools.math.Function1D;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class BroadbandFunction {
   private final double[] hz;
   private final double[] values;
   private final Function1D function;

   public BroadbandFunction(double[] hz, double[] values) {
      this.hz = hz;
      this.values = values;
      function = Function1D.interpolate(hz, values);
   }

   static BroadbandFunction parse(Element element, String attributeName) {
      List<Element> caseElements = element.elements(CalibrationXml.CASE);
      Point2D.Double[] points = new Point2D.Double[caseElements.size()];
      for (int i = 0; i < points.length; i++) {
         Element caseElement = caseElements.get(i);
         double hz = Double.parseDouble(caseElement.attributeValue(CalibrationXml.HZ));
         double value = Double.parseDouble(caseElement.attributeValue(attributeName));
         points[i] = new Point2D.Double(hz, value);
      }
      Arrays.sort(points, Comparator.comparingDouble(Point2D.Double::getX));

      double[] hz = new double[points.length];
      double[] values = new double[points.length];
      for (int i = 0; i < points.length; i++) {
         Point2D.Double point = points[i];
         hz[i] = point.x;
         values[i] = point.y;
      }
      return new BroadbandFunction(hz, values);
   }

   void addXml(Element element, String name) {
      for (int i = 0; i < hz.length; i++) {
         element.addElement(CalibrationXml.CASE)
               .addAttribute(CalibrationXml.HZ, Utils.toString(hz[i]))
               .addAttribute(name, Utils.toString(values[i]));
      }
   }

   public double[] getHz() {
      return hz;
   }

   public double[] getValues() {
      return values;
   }

   public double getValue(double aHz) {
      return function.eval(aHz);
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof BroadbandFunction that
            && Arrays.equals(hz, that.hz)
            && Arrays.equals(values, that.values);
      // Skip `function` since it is derived from `hz` and `values`.
   }

   @Override
   public int hashCode() {
      int result = Arrays.hashCode(hz);
      result = 31 * result + Arrays.hashCode(values);
      // Skip `function` since it is derived from `hz` and `values`.
      return result;
   }
}
