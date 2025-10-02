package no.imr.tools.plot;

import org.jfree.data.xy.XYZDataset;

/**
 * Base class for XYZ data sets.
 */
public abstract class BaseXYZDataset extends BaseXYDataset implements XYZDataset, XYZInfoContainer {
   protected BaseXYZDataset() {
   }

   @Override
   public final Number getZ(int series, int item) {
      return getZValue(series, item);
   }

   @Override
   public abstract double getZValue(int series, int item);
}
