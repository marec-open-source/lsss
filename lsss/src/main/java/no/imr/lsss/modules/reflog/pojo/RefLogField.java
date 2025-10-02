package no.imr.lsss.modules.reflog.pojo;

import org.jspecify.annotations.Nullable;

public final class RefLogField {
   public @Nullable String name;
   public @Nullable Number value;
   public @Nullable Object extendedValue;

   public RefLogField() {
   }

   @Override
   public String toString() {
      return "RefLogField{" +
            "name='" + name + '\'' +
            ", value=" + value +
            '}';
   }
}
