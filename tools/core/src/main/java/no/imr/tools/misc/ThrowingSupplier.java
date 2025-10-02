package no.imr.tools.misc;

@FunctionalInterface
public interface ThrowingSupplier<T, E extends Throwable> {
   T get() throws E;
}
