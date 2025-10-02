package no.imr.tools;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class LateInit<T> {
   private @Nullable T value;

   public LateInit() {
   }

   public void init(T value) {
      if (this.value != null) {
         throw new IllegalStateException();
      }
      this.value = value;
   }

   public void set(T value) {
      this.value = value;
   }

   public T get() {
      return Objects.requireNonNull(value);
   }
}
