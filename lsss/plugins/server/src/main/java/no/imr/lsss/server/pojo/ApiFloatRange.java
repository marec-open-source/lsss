package no.imr.lsss.server.pojo;

import no.imr.tools.annotations.ReflectionEntryPoint;
import no.imr.tools.range.FloatRange;

public final class ApiFloatRange {
   public float min;
   public float max;

   @ReflectionEntryPoint
   public ApiFloatRange() {
   }

   public ApiFloatRange(FloatRange range) {
      min = range.min();
      max = range.max();
   }

   public FloatRange toFloatRange() {
      return FloatRange.of(min, max);
   }

   @Override
   public String toString() {
      return "ApiFloatRange{" +
            "min=" + min +
            ", max=" + max +
            '}';
   }
}
