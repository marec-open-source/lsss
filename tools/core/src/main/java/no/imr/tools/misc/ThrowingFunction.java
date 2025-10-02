package no.imr.tools.misc;

import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface ThrowingFunction<T, R extends @Nullable Object, E extends Throwable> {
   R apply(T t) throws E;
}
