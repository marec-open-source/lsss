package no.imr.tools.plot;

import no.imr.tools.SmartNumberFormat;
import no.imr.tools.math.NiceNumber;
import no.imr.tools.range.FloatRange;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTick;
import org.jfree.chart.axis.Tick;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.Range;
import org.jspecify.annotations.Nullable;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.LongStream;

public class TransformedNumberAxis extends NumberAxis {
   private final DoubleUnaryOperator transform;
   private final DoubleUnaryOperator inverse;

   public TransformedNumberAxis(DoubleUnaryOperator transform, DoubleUnaryOperator inverse) {
      this(null, transform, inverse);
   }

   public TransformedNumberAxis(@Nullable String label, DoubleUnaryOperator transform, DoubleUnaryOperator inverse) {
      super(label);

      this.transform = transform;
      this.inverse = inverse;
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof TransformedNumberAxis that
            && super.equals(that)
            && transform.equals(that.transform)
            && inverse.equals(that.inverse);
   }

   @Override
   public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + transform.hashCode();
      result = 31 * result + inverse.hashCode();
      return result;
   }

   @Override
   public double valueToJava2D(double value, Rectangle2D area, RectangleEdge edge) {
      value = transform.applyAsDouble(value);

      Range range = getRange();
      double axisMin = transform.applyAsDouble(range.getLowerBound());
      double axisMax = transform.applyAsDouble(range.getUpperBound());

      double min;
      double max;
      if (RectangleEdge.isTopOrBottom(edge)) {
         min = area.getMinX();
         max = area.getMaxX();
      } else if (RectangleEdge.isLeftOrRight(edge)) {
         max = area.getMinY();
         min = area.getMaxY();
      } else {
         throw new UnsupportedOperationException(edge.toString());
      }

      if (isInverted()) {
         return max - ((value - axisMin) / (axisMax - axisMin)) * (max - min);
      } else {
         return min + ((value - axisMin) / (axisMax - axisMin)) * (max - min);
      }
   }

   @Override
   public double java2DToValue(double java2DValue, Rectangle2D area, RectangleEdge edge) {
      Range range = getRange();
      double axisMin = transform.applyAsDouble(range.getLowerBound());
      double axisMax = transform.applyAsDouble(range.getUpperBound());

      double min;
      double max;
      if (RectangleEdge.isTopOrBottom(edge)) {
         min = area.getMinX();
         max = area.getMaxX();
      } else if (RectangleEdge.isLeftOrRight(edge)) {
         min = area.getMaxY();
         max = area.getMinY();
      } else {
         throw new UnsupportedOperationException(edge.toString());
      }

      double value;
      if (isInverted()) {
         value = axisMax - (java2DValue - min) / (max - min) * (axisMax - axisMin);
      } else {
         value = axisMin + (java2DValue - min) / (max - min) * (axisMax - axisMin);
      }

      return inverse.applyAsDouble(value);
   }

   @Override
   protected List<Tick> refreshTicksHorizontal(Graphics2D g2, Rectangle2D dataArea, RectangleEdge edge) {
      return makeTicks(dataArea.getWidth(), TextAnchor.TOP_CENTER);
   }

   @Override
   protected List<Tick> refreshTicksVertical(Graphics2D g2, Rectangle2D dataArea, RectangleEdge edge) {
      return makeTicks(dataArea.getHeight(), TextAnchor.CENTER_RIGHT);
   }

   private List<Tick> makeTicks(double pixelSize, TextAnchor textAnchor) {
      Range range = getRange();
      long n = Math.max(3, Math.round(pixelSize / 50));
      double delta = NiceNumber.niceNumber(range.getLength() / n, false);
      NumberFormat numberFormat = Objects.requireNonNullElseGet(getNumberFormatOverride(), SmartNumberFormat::new);
      long iMin = (long) Math.ceil(range.getLowerBound() / delta);
      long iMax = (long) Math.floor(range.getUpperBound() / delta);
      return LongStream.rangeClosed(iMin, iMax)
            .<Tick>mapToObj(i -> {
               double value = i * delta;
               String label = numberFormat.format(value);
               return new NumberTick(value, label, textAnchor, TextAnchor.CENTER, 0);
            })
            .toList();
   }

   @Override
   public void zoomRange(double lowerPercent, double upperPercent) {
      Range range = getRange();
      double start = transform.applyAsDouble(range.getLowerBound());
      double length = transform.applyAsDouble(range.getUpperBound()) - start;
      double r0;
      double r1;
      if (isInverted()) {
         r0 = start + (length * (1 - upperPercent));
         r1 = start + (length * (1 - lowerPercent));
      } else {
         r0 = start + length * lowerPercent;
         r1 = start + length * upperPercent;
      }
      r0 = inverse.applyAsDouble(r0);
      r1 = inverse.applyAsDouble(r1);
      if (r1 > r0 && !Double.isInfinite(r1 - r0)) {
         setRange(new Range(r0, r1));
      }
   }

   @Override
   public void resizeRange(double percent) {
      Range range = getRange();
      double min = transform.applyAsDouble(range.getLowerBound());
      double max = transform.applyAsDouble(range.getUpperBound());
      double center = (min + max) / 2;
      resizeRange2(percent, inverse.applyAsDouble(center));
   }

   @Override
   public void resizeRange(double percent, double anchorValue) {
      resizeRange2(percent, anchorValue);
   }

   @Override
   public void resizeRange2(double percent, double anchorValue) {
      if (percent > 0) {
         Range range = getRange();
         double min = transform.applyAsDouble(range.getLowerBound());
         double max = transform.applyAsDouble(range.getUpperBound());
         double ref = transform.applyAsDouble(anchorValue);
         double newMin = ref - percent * (ref - min);
         double newMax = ref + percent * (max - ref);
         setRange(new Range(inverse.applyAsDouble(newMin), inverse.applyAsDouble(newMax)));
      } else {
         setAutoRange(true);
      }
   }

   public void setMargins(FloatRange range, double factor) {
      if (range.isEmpty()) {
         setLowerMargin(0);
         setUpperMargin(0);
         return;
      }
      double min = transform.applyAsDouble(range.min());
      double max = transform.applyAsDouble(range.max());
      double delta = (max - min) * factor;
      setLowerMargin((range.min() - inverse.applyAsDouble(min - delta)) / range.getSize());
      setUpperMargin((inverse.applyAsDouble(max + delta) - range.max()) / range.getSize());
   }
}
