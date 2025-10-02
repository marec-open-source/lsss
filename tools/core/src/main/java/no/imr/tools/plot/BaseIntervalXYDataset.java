package no.imr.tools.plot;

import org.jfree.data.xy.AbstractIntervalXYDataset;

public abstract class BaseIntervalXYDataset extends AbstractIntervalXYDataset implements XYInfoContainer {
   protected BaseIntervalXYDataset() {
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

   @Override
   public final Number getStartX(int series, int item) {
      return getStartXValue(series, item);
   }

   @Override
   public abstract double getStartXValue(int series, int item);

   @Override
   public final Number getEndX(int series, int item) {
      return getEndXValue(series, item);
   }

   @Override
   public abstract double getEndXValue(int series, int item);

   @Override
   public final Number getStartY(int series, int item) {
      return getStartYValue(series, item);
   }

   @Override
   public abstract double getStartYValue(int series, int item);

   @Override
   public final Number getEndY(int series, int item) {
      return getEndYValue(series, item);
   }

   @Override
   public abstract double getEndYValue(int series, int item);
}
