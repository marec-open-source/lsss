package no.imr.korona.data.util;

public final class TvgArray {
   private final float[] values;
   private final int offset;

   TvgArray(float[] values, int offset) {
      this.values = values;
      this.offset = offset;
   }

   public float get(int index) {
      return values[Math.max(0, offset + index)];
   }
}
