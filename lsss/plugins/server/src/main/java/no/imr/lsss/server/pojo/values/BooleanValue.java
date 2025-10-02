package no.imr.lsss.server.pojo.values;

import no.imr.tools.annotations.ReflectionEntryPoint;

public final class BooleanValue {
   public boolean value;

   @ReflectionEntryPoint
   public BooleanValue() {
   }

   public BooleanValue(boolean value) {
      this.value = value;
   }

   @Override
   public String toString() {
      return "BooleanValue{" +
            "value=" + value +
            '}';
   }
}
