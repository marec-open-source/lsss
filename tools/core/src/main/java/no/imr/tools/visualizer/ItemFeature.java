package no.imr.tools.visualizer;

import no.imr.tools.Utils;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.PlotUtils;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTick;
import org.jfree.chart.axis.Tick;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.TextAnchor;
import org.jspecify.annotations.Nullable;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract sealed class ItemFeature<T> {
   public final String name;
   public final Unit unit;
   public final ToDoubleFunction<T> itemToDouble;
   public final Function<T, String> itemToString;

   private ItemFeature(String name, Unit unit, ToDoubleFunction<T> itemToDouble, Function<T, String> itemToString) {
      this.name = name;
      this.unit = unit;
      this.itemToDouble = itemToDouble;
      this.itemToString = itemToString;
   }

   @Override
   public String toString() {
      return name;
   }

   public String getNameAndUnit() {
      return Utils.nameAndUnit(name, unit);
   }

   ValueAxis toAxis(boolean horizontal) {
      return PlotUtils.newNumberAxis(null);
   }

   public static final class Text<T> extends ItemFeature<T> {
      public Text(String name, Function<T, String> itemToString) {
         super(name, Unit.NONE, _ -> Double.NaN, itemToString);
      }
   }

   public static final class Number<T> extends ItemFeature<T> {
      public Number(String name, Unit unit, ToDoubleFunction<T> itemToDouble) {
         this(name, unit, itemToDouble, item -> Double.toString(itemToDouble.applyAsDouble(item)));
      }

      public Number(String name, Unit unit, ToDoubleFunction<T> itemToDouble, NumberFormat numberFormat) {
         this(name, unit, itemToDouble, item -> numberFormat.format(itemToDouble.applyAsDouble(item)));
      }

      public Number(String name, Unit unit, ToDoubleFunction<T> itemToDouble, Function<T, String> itemToString) {
         super(name, unit, itemToDouble, itemToString);
      }
   }

   public static final class Int<T> extends ItemFeature<T> {
      public Int(String name, Unit unit, ToLongFunction<T> itemToInt) {
         super(name, unit, itemToInt::applyAsLong, item -> Long.toString(itemToInt.applyAsLong(item)));
      }

      @Override
      ValueAxis toAxis(boolean horizontal) {
         NumberAxis axis = PlotUtils.newNumberAxis(null);
         axis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
         return axis;
      }
   }

   public static final class Time<T> extends ItemFeature<T> {
      final DateTimeFormatter dateTimeFormatter;

      private Time(String name, Unit unit, ToDoubleFunction<T> itemToDouble, Function<T, String> itemToString, DateTimeFormatter dateTimeFormatter) {
         super(name, unit, itemToDouble, itemToString);

         this.dateTimeFormatter = dateTimeFormatter;
      }

      public static <T> Time<T> fromInstant(String name, Unit unit, Function<T, @Nullable Instant> itemToInstant, DateTimeFormatter dateTimeFormatter) {
         ToDoubleFunction<T> itemToDouble = item -> {
            Instant instant = itemToInstant.apply(item);
            if (instant != null) {
               try {
                  return instant.toEpochMilli();
               } catch (ArithmeticException _) {
                  // Cannot be represented as a long.
               }
            }
            return Double.NaN;
         };
         Function<T, String> itemToString = item -> {
            Instant instant = itemToInstant.apply(item);
            return instant != null ? dateTimeFormatter.format(instant) : "";
         };
         return new Time<>(name, unit, itemToDouble, itemToString, dateTimeFormatter);
      }

      @Override
      ValueAxis toAxis(boolean horizontal) {
         return new DateAxis(null, TimeZone.getTimeZone(dateTimeFormatter.getZone()), Locale.ENGLISH);
      }
   }

   public static final class Category<T> extends ItemFeature<T> {
      private final boolean rotateHorizontalLabels;
      private final CategoryToIndexFunction<T> categoryToIndexFunction;
      private List<String> categories = List.of();

      public Category(String name, List<String> categories, Function<T, String> itemToCategory, boolean rotateHorizontalLabels) {
         this(name, new CategoryToIndexFunction<>(itemToCategory), itemToCategory, rotateHorizontalLabels);

         setCategories(categories);
      }

      private Category(String name, CategoryToIndexFunction<T> categoryToIndexFunction, Function<T, String> itemToCategory, boolean rotateHorizontalLabels) {
         super(name, Unit.NONE, categoryToIndexFunction, itemToCategory);

         this.rotateHorizontalLabels = rotateHorizontalLabels;
         this.categoryToIndexFunction = categoryToIndexFunction;
      }

      List<String> getCategories() {
         return categories;
      }

      public void setCategories(List<String> categories) {
         this.categories = categories;
         categoryToIndexFunction.update(categories);
      }

      @Override
      ValueAxis toAxis(boolean horizontal) {
         NumberAxis axis = new NumberAxis() {
            @Override
            public List<Tick> refreshTicks(Graphics2D g2, AxisState state, Rectangle2D dataArea, RectangleEdge edge) {
               return IntStream.range(0, categories.size())
                     .<Tick>mapToObj(i -> new NumberTick(i, categories.get(i),
                           horizontal ? (rotateHorizontalLabels ? TextAnchor.CENTER_LEFT : TextAnchor.TOP_CENTER) : TextAnchor.CENTER_RIGHT,
                           TextAnchor.CENTER_LEFT,
                           horizontal && rotateHorizontalLabels ? Math.toRadians(90) : 0))
                     .toList();
            }
         };
         if (horizontal && rotateHorizontalLabels) {
            axis.setVerticalTickLabels(true);
         }
         axis.setRange(-1, categories.size());
         return axis;
      }

      private static final class CategoryToIndexFunction<T> implements ToDoubleFunction<T> {
         private final Function<T, String> itemToCategory;
         private Map<String, Integer> categoryToIndex = Map.of();

         private CategoryToIndexFunction(Function<T, String> itemToCategory) {
            this.itemToCategory = itemToCategory;
         }

         private void update(List<String> categories) {
            categoryToIndex = IntStream.range(0, categories.size())
                  .boxed()
                  .collect(Collectors.toUnmodifiableMap(categories::get, Function.identity()));
         }

         @Override
         public double applyAsDouble(T item) {
            Integer index = categoryToIndex.get(itemToCategory.apply(item));
            return index != null ? index : Double.NaN;
         }
      }
   }
}
