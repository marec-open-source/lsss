package no.imr.lsss.server.pojo.values;

import no.imr.lsss.server.pojo.ApiFloatRange;
import no.imr.tools.annotations.ReflectionEntryPoint;

public final class FloatRangeValue {
   public ApiFloatRange value = new ApiFloatRange();

   @ReflectionEntryPoint
   public FloatRangeValue() {
   }

   @Override
   public String toString() {
      return "FloatRangeValue{" +
            "value=" + value +
            '}';
   }
}
