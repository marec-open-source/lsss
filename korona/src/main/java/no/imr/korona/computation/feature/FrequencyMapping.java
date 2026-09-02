package no.imr.korona.computation.feature;

import no.imr.tools.plot.TransformedNumberAxis;
import no.imr.tools.range.FloatRange;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.NumberTick;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.TextAnchor;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

/**
 * Maps frequency in kHz to some scale.
 */
public enum FrequencyMapping {
   IDENTITY(DoubleUnaryOperator.identity(), DoubleUnaryOperator.identity()),
   SQRT(x -> x >= 0 ? Math.sqrt(x) : -Math.sqrt(-x), x -> x >= 0 ? x * x : -x * x),
   CBRT(Math::cbrt, x -> x * x * x),
   LOG(Math::log, Math::exp);

   private final DoubleUnaryOperator kHzToX;
   private final DoubleUnaryOperator xToKHz;

   FrequencyMapping(DoubleUnaryOperator kHzToX, DoubleUnaryOperator xToKHz) {
      this.kHzToX = kHzToX;
      this.xToKHz = xToKHz;
   }

   public TransformedNumberAxis axis(FloatRange kHzRange) {
      return axis(kHzRange, Set.of());
   }

   public TransformedNumberAxis axis(FloatRange kHzRange, Set<Integer> kHzTicks) {
      TransformedNumberAxis axis = new FrequencyAxis(kHzTicks, kHzToX, xToKHz);
      axis.setAutoRangeIncludesZero(false);
      axis.setMargins(kHzRange, 0.05);
      return axis;
   }

   private static final class FrequencyAxis extends TransformedNumberAxis {
      private final Set<Integer> kHzTicks;

      private FrequencyAxis(Set<Integer> kHzTicks, DoubleUnaryOperator kHzToX, DoubleUnaryOperator xToKHz) {
         super("Frequency [kHz]", kHzToX, xToKHz);

         this.kHzTicks = kHzTicks;
      }

      @Override
      public List<?> refreshTicks(Graphics2D g2, AxisState state, Rectangle2D dataArea, RectangleEdge edge) {
         List<NumberTick> ticks = kHzTicks.stream()
               .filter(getRange()::contains)
               .map(kHz -> new NumberTick(kHz, Integer.toString(kHz), TextAnchor.TOP_CENTER, TextAnchor.CENTER, 0))
               .toList();
         if (ticks.size() <= 1) {
            return super.refreshTicks(g2, state, dataArea, edge);
         }
         return ticks;
      }
   }
}
