package no.imr.korona.computation.feature;

import no.imr.tools.range.FloatRange;

public final class CellAveragedSv {
   private final int frequencyIndex;
   private final String name;
   private double sumSv;
   private int count;
   private final FloatRange range;

   public CellAveragedSv(int frequencyIndex, String name, FloatRange range) {
      this.frequencyIndex = frequencyIndex;
      this.name = name;
      this.range = range;
   }

   public String getName() {
      return name;
   }

   public int getFrequencyIndex() {
      return frequencyIndex;
   }

   public void update(float svValue, float r) {
      if (!range.contains(r)) {
         return;
      }
      sumSv += svValue;
      count++;
   }

   public void update(CellAveragedSv cellAveragedSv) {
      sumSv += cellAveragedSv.sumSv;
      count += cellAveragedSv.count;
   }

   public void mergeWith(CellAveragedSv otherCell) {
      count += otherCell.count;
      sumSv += otherCell.sumSv;
   }

   public int getCount() {
      return count;
   }

   public float getFillFactor(int maxFillCount) {
      return (float) count / maxFillCount;
   }

   public double getMeanSv() {
      if (count == 0) {
         throw new IllegalStateException("No values added to cell");
      }
      return sumSv / count;
   }

   public void clear() {
      count = 0;
      sumSv = 0;
   }
}
