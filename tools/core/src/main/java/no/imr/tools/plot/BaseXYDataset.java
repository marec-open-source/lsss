package no.imr.tools.plot;

import org.jfree.data.xy.AbstractXYDataset;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class BaseXYDataset extends AbstractXYDataset implements XYInfoContainer {
   protected BaseXYDataset() {
   }

   @Override
   public String toString() {
      return IntStream.range(0, getSeriesCount())
            .mapToObj(i -> getSeriesKey(i) + " (" + getItemCount(i) + ")")
            .collect(Collectors.joining(", "));
   }

   @Override
   public final Number getX(int series, int item) {
      return getXValue(series, item);
   }

   @Override
   public abstract double getXValue(int series, int item);

   @Override
   public final Number getY(int series, int item) {
      return getYValue(series, item);
   }

   @Override
   public abstract double getYValue(int series, int item);
}
