package no.imr.tools.math;

import no.imr.tools.range.FloatRange;

public final class Histogram2D {
   private final float minX;
   private final float deltaX;
   private final float minY;
   private final float deltaY;
   private final int[][] counts;

   public Histogram2D(FloatRange rangeX, float deltaX, FloatRange rangeY, float deltaY) {
      FloatRange roundedRangeX = rangeX.expandToMultipleOf(deltaX);
      minX = roundedRangeX.min();
      this.deltaX = deltaX;

      FloatRange roundedRangeY = rangeY.expandToMultipleOf(deltaY);
      minY = roundedRangeY.min();
      this.deltaY = deltaY;

      int nx = Math.round(roundedRangeX.getSize() / deltaX);
      int ny = Math.round(roundedRangeY.getSize() / deltaY);
      counts = new int[nx][ny];
   }

   public float getMinX() {
      return minX;
   }

   public float getDeltaX() {
      return deltaX;
   }

   public float getMinY() {
      return minY;
   }

   public float getDeltaY() {
      return deltaY;
   }

   public int[][] getCounts() {
      return counts;
   }

   public int xToI(float x) {
      return (int) Math.floor((x - minX) / deltaX);
   }

   public int yToJ(float y) {
      return (int) Math.floor((y - minY) / deltaY);
   }

   public void addByXY(float x, float y) {
      int i = xToI(x);
      if (i < 0 || i >= counts.length) {
         return;
      }
      int[] iCounts = counts[i];

      int j = yToJ(y);
      if (j < 0 || j >= iCounts.length) {
         return;
      }
      iCounts[j]++;
   }

   public void addByIndex(int i, int j) {
      if (i < 0 || i >= counts.length) {
         return;
      }
      int[] iCounts = counts[i];
      if (j < 0 || j >= iCounts.length) {
         return;
      }
      iCounts[j]++;
   }
}
