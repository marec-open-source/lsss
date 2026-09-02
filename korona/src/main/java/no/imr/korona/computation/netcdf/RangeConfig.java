package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.items.channel.ChannelData;
import org.jspecify.annotations.Nullable;
import ucar.ma2.ArrayDouble;

record RangeConfig(
      float min,
      float delta,
      int length
) {
   ArrayDouble.D1 toArray() {
      ArrayDouble.D1 ranges = new ArrayDouble.D1(length);
      for (int i = 0; i < length; i++) {
         ranges.set(i, min + i * delta);
      }
      return ranges;
   }

   float centerRange(int rangeIndex) {
      return min + (rangeIndex + 0.5f) * delta;
   }

   @Nullable OffsetValues resample(ChannelData channelData, float[] values) {
      return OffsetValues.resample(values, channelData, min, delta, length);
   }
}
