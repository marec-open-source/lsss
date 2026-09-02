package no.imr.korona.computation.feature;

import no.imr.tools.SmartNumberFormat;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.plot.TransformedNumberAxis;
import no.imr.tools.range.FloatRange;
import org.jfree.chart.axis.NumberAxis;

import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

public enum FrequencyResponseAxis {
   DYNAMIC(range -> {
      NumberAxis axis = PlotUtils.newNumberAxis(null);
      axis.setAutoRangeIncludesZero(true);
      axis.setRange(0, range.max());
      return axis;
   }),

   FIXED(_ -> DYNAMIC.axis(FloatRange.of(0, 6))),

   SQRT(range -> transformedAxis(range,
         x -> x >= 0 ? Math.sqrt(x) : -Math.sqrt(-x),
         x -> x >= 0 ? x * x : -x * x)),

   CBRT(range -> transformedAxis(range, Math::cbrt, x -> x * x * x)),

   LOG(range -> transformedAxis(range, Math::log10, x -> Math.pow(10, x)));

   private final Function<FloatRange, NumberAxis> rangeToAxis;

   FrequencyResponseAxis(Function<FloatRange, NumberAxis> rangeToAxis) {
      this.rangeToAxis = rangeToAxis;
   }

   public NumberAxis axis(FloatRange range) {
      return rangeToAxis.apply(range);
   }

   private static TransformedNumberAxis transformedAxis(FloatRange range, DoubleUnaryOperator transform, DoubleUnaryOperator inverse) {
      TransformedNumberAxis axis = new TransformedNumberAxis(transform, inverse);
      axis.setNumberFormatOverride(new SmartNumberFormat());
      axis.setAutoRangeIncludesZero(false);
      axis.setMargins(range, 0.05);
      return axis;
   }
}
