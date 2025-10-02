package no.imr.lsss.modules.interpretation;

import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.tools.range.FloatRange;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A grid cell.
 */
public final class GridCell {
   private final GridColumn gridColumn;
   private final int yIndex;
   private final FloatRange zRange;
   private final float[] totalAssignedSa;
   private final Map<AcousticCategory, CategorySa> categoryToSa = new HashMap<>();

   GridCell(GridColumn gridColumn, int yIndex, FloatRange zRange) {
      this.gridColumn = gridColumn;
      this.yIndex = yIndex;
      this.zRange = zRange;
      totalAssignedSa = new float[gridColumn.getGrid().getTransducerCount()];
   }

   public int getChannelNumber() {
      if (gridColumn.getGrid().isPelagic()) {
         return yIndex + 1;
      } else {
         return -yIndex;
      }
   }

   public FloatRange getZRange() {
      return zRange;
   }

   public float getTotalAssignedSa(int channel) {
      return totalAssignedSa[channel - 1];
   }

   public Collection<AcousticCategory> getAcousticCategories() {
      return categoryToSa.keySet();
   }

   public float getCategorySa(AcousticCategory acousticCategory, int channel) {
      CategorySa categorySa = categoryToSa.get(acousticCategory);
      return categorySa != null ? categorySa.sa[channel - 1] : 0;
   }

   void addSa(int channel, AcousticCategory acousticCategory, float assignedVerticalIntegral) {
      if (!acousticCategory.rawData()) {
         totalAssignedSa[channel - 1] += assignedVerticalIntegral;
      }

      CategorySa categorySa = categoryToSa.get(acousticCategory);
      if (categorySa == null) {
         categorySa = new CategorySa(totalAssignedSa.length);
         categoryToSa.put(acousticCategory, categorySa);
      }
      categorySa.sa[channel - 1] += assignedVerticalIntegral;
   }

   void integrationFinished(float bubbleCorrection, double[] accumulatedDistance) {
      for (int i = 0; i < totalAssignedSa.length; i++) {
         double distance = accumulatedDistance[i];
         if (distance != 0) {
            totalAssignedSa[i] *= (float) (bubbleCorrection / distance);
         }
      }

      for (CategorySa categorySa : categoryToSa.values()) {
         categorySa.integrationFinished(bubbleCorrection, accumulatedDistance);
      }
   }

   private static final class CategorySa {
      private final float[] sa;

      private CategorySa(int transducerCount) {
         sa = new float[transducerCount];
      }

      private void integrationFinished(float bubbleCorrection, double[] accumulatedDistance) {
         for (int i = 0; i < sa.length; i++) {
            double distance = accumulatedDistance[i];
            if (distance != 0) {
               sa[i] *= (float) (bubbleCorrection / distance);
            }
         }
      }
   }
}
