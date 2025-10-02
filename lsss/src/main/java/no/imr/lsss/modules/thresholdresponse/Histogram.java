package no.imr.lsss.modules.thresholdresponse;

/**
 * Histogram for computing the threshold response.
 * <p>
 * Cell <code>i</code> has range
 * <pre>[ MIN_LOG_SV + (i - 1) * DELTA_LOG_SV, MIN_LOG_SV + i * DELTA_LOG_SV )</pre>
 * except for the leftmost cell, i = 0, that has range
 * <pre>( -inf, MIN_LOG_SV )</pre>
 * and for the rightmost cell, i = CELL_COUNT - 1, that has range
 * <pre>[ MAX_LOG_SV, +inf )</pre>
 * <p>
 * dB values higher than MAX_LOG_SV are ignored.
 */
final class Histogram {
   static final float MIN_LOG_SV = -80;
   static final float MAX_LOG_SV = -20;
   private static final float DELTA_LOG_SV = 1;
   private static final float RANGE_LOG_SV = MAX_LOG_SV - MIN_LOG_SV;
   static final int CELL_COUNT = (int) (RANGE_LOG_SV / DELTA_LOG_SV) + 2;
   private static final float[] CELL_BOUNDARIES = new float[CELL_COUNT + 1];

   static {
      CELL_BOUNDARIES[0] = Float.NEGATIVE_INFINITY;
      for (int i = 1; i < CELL_COUNT; i++) {
         CELL_BOUNDARIES[i] = MIN_LOG_SV + i * DELTA_LOG_SV;
      }
      CELL_BOUNDARIES[CELL_COUNT] = Float.POSITIVE_INFINITY;
   }

   private Histogram() {
   }

   static int logSvToIndex(float logSv) {
      int i = (int) Math.floor((logSv - MIN_LOG_SV) / DELTA_LOG_SV);
      return Math.clamp(i, 0, CELL_COUNT - 1);
   }

   static float getLowerCellBoundary(int index) {
      return CELL_BOUNDARIES[index];
   }
}
