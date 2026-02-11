package no.imr.korona.computation.netcdf;

import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class OffsetValuesTest {
   @Test
   void resample() {
      assertNull(OffsetValues.resample(new float[]{5, 6, 7}, FloatRange.of(5, 8), FloatRange.of(2, 5), 3));
      check(2, new float[]{5}, OffsetValues.resample(new float[]{5, 6, 7}, FloatRange.of(5, 8), FloatRange.of(3, 6), 3));
      check(1, new float[]{5, 6}, OffsetValues.resample(new float[]{5, 6, 7}, FloatRange.of(5, 8), FloatRange.of(4, 7), 3));
      check(0, new float[]{5, 6, 7}, OffsetValues.resample(new float[]{5, 6, 7}, FloatRange.of(5, 8), FloatRange.of(5, 8), 3));
      check(0, new float[]{6, 7}, OffsetValues.resample(new float[]{5, 6, 7}, FloatRange.of(5, 8), FloatRange.of(6, 9), 3));
      check(0, new float[]{7}, OffsetValues.resample(new float[]{5, 6, 7}, FloatRange.of(5, 8), FloatRange.of(7, 10), 3));
      assertNull(OffsetValues.resample(new float[]{5, 6, 7}, FloatRange.of(5, 8), FloatRange.of(8, 11), 3));

      check(0, new float[]{3.5f, 5.5f}, OffsetValues.resample(new float[]{1, 2, 3, 4, 5, 6}, FloatRange.of(-1, 2), FloatRange.of(0, 10), 10));
      check(5, new float[]{1.5f, 3.5f, 5.5f}, OffsetValues.resample(new float[]{1, 2, 3, 4, 5, 6}, FloatRange.of(5, 8), FloatRange.of(0, 10), 10));
      check(6, new float[]{2.5f, 4.5f}, OffsetValues.resample(new float[]{1, 2, 3, 4, 5, 6}, FloatRange.of(5.5f, 8.5f), FloatRange.of(0, 10), 10));
      check(8, new float[]{1.5f, 3.5f}, OffsetValues.resample(new float[]{1, 2, 3, 4, 5, 6}, FloatRange.of(8, 11), FloatRange.of(0, 10), 10));
   }

   private static void check(int offset, float[] values, @Nullable OffsetValues actual) {
      assertNotNull(actual);
      assertEquals(offset, actual.offset());
      assertArrayEquals(values, actual.values());
   }
}
