package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

final class OffsetValues {
   final int offset;
   final float[] values;

   private OffsetValues(int offset, float[] values) {
      this.offset = offset;
      this.values = values;
   }

   @Override
   public String toString() {
      return "offset: " + offset + ", length:" + values.length;
   }

   private static @Nullable OffsetValues newOffsetValues(int offset, float[] values) {
      if (values.length == 0) {
         return null;
      }
      return new OffsetValues(offset, values);
   }

   private static @Nullable OffsetValues create(int offset, float[] values, int maxLength) {
      if (offset < 0) {
         if (-offset >= values.length) {
            return null;
         }
         return newOffsetValues(0, Arrays.copyOfRange(values, -offset, Math.min(-offset + maxLength, values.length)));
      }
      if (offset >= maxLength) {
         return null;
      }
      if (offset + values.length > maxLength) {
         return newOffsetValues(offset, Arrays.copyOfRange(values, 0, maxLength - offset));
      }
      return newOffsetValues(offset, values);
   }

   static @Nullable OffsetValues resample(float[] values, ChannelData channelData, float targetSampleDistance, int targetLength) {
      if (channelData.getSampleDistance() == targetSampleDistance) {
         return create(channelData.getOffset(), values, targetLength);
      }
      FloatRange valuesRange = FloatRange.of(channelData.getMinRange(), channelData.getMaxRange());
      FloatRange targetRange = FloatRange.of(0, targetSampleDistance * targetLength);
      return resample(values, valuesRange, targetRange, targetLength);
   }

   static @Nullable OffsetValues resample(float[] values, FloatRange valuesRange, FloatRange targetRange, int targetLength) {
      // Target index range:
      int targetBeginIndex = (int) Math.ceil(Math.clamp(targetRange.valueToFraction(valuesRange.min()), 0, 1) * targetLength);
      int targetEndIndex = (int) Math.floor(Math.clamp(targetRange.valueToFraction(valuesRange.max()), 0, 1) * targetLength);

      // Value range:
      float beginValue = targetRange.fractionToValue((float) targetBeginIndex / targetLength);
      float endValue = targetRange.fractionToValue((float) targetEndIndex / targetLength);

      // Source index range:
      float valuesBeginIndex = valuesRange.valueToFraction(beginValue) * values.length;
      float valuesEndIndex = valuesRange.valueToFraction(endValue) * values.length;

      // Resample:
      float[] resampledValues = ArrayMath.resample(values, valuesBeginIndex, valuesEndIndex, targetEndIndex - targetBeginIndex);
      return create(targetBeginIndex, resampledValues, targetLength);
   }
}
