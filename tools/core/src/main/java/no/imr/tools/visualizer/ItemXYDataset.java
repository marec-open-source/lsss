package no.imr.tools.visualizer;

import no.imr.tools.plot.BaseXYDataset;

import java.util.List;
import java.util.function.ToDoubleFunction;

final class ItemXYDataset<T> extends BaseXYDataset {
   private final String name;
   private final List<T> items;
   private final ToDoubleFunction<T> xExtractor;
   private final ToDoubleFunction<T> yExtractor;

   ItemXYDataset(String name, List<T> items, ToDoubleFunction<T> xExtractor, ToDoubleFunction<T> yExtractor) {
      this.name = name;
      this.items = items;
      this.xExtractor = xExtractor;
      this.yExtractor = yExtractor;
   }

   List<T> getItems() {
      return items;
   }

   @Override
   public int getSeriesCount() {
      return 1;
   }

   @Override
   public String getSeriesKey(int series) {
      return name;
   }

   @Override
   public int getItemCount(int series) {
      return items.size();
   }

   @Override
   public double getXValue(int series, int item) {
      return xExtractor.applyAsDouble(items.get(item));
   }

   @Override
   public double getYValue(int series, int item) {
      return yExtractor.applyAsDouble(items.get(item));
   }
}
