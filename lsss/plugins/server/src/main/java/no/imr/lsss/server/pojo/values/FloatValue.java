package no.imr.lsss.server.pojo.values;

import no.imr.tools.annotations.ReflectionEntryPoint;

public final class FloatValue {
   public float value;

   @ReflectionEntryPoint
   public FloatValue() {
   }

   public FloatValue(float value) {
      this.value = value;
   }

   @Override
   public String toString() {
      return "FloatValue{" +
            "value=" + value +
            '}';
   }
}
