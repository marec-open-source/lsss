package no.imr.tools.misc;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public final class ClearableMemoizingSupplier<T> implements Supplier<T> {
   private final Supplier<T> supplier;
   private volatile @Nullable T value;

   public ClearableMemoizingSupplier(Supplier<T> supplier) {
      this.supplier = supplier;
   }

   @Override
   public T get() {
      T result = value;
      if (result == null) {
         synchronized (this) { // Double-checked locking with volatile field.
            result = value;
            if (result == null) {
               result = supplier.get();
               value = result;
            }
         }
      }
      return result;

   }

   public void clear() {
      value = null;
   }
}
