package no.imr.lsss.server.pojo.values;

import no.imr.tools.annotations.ReflectionEntryPoint;
import org.jspecify.annotations.Nullable;

public final class ObjectValue {
   public @Nullable Object value;

   @ReflectionEntryPoint
   public ObjectValue() {
   }

   public ObjectValue(@Nullable Object value) {
      this.value = value;
   }

   @Override
   public String toString() {
      return "ObjectValue{" +
            "value=" + value +
            '}';
   }
}
