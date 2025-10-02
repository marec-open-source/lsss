package no.imr.tools.misc;

@FunctionalInterface
public interface ToFloatFunction<T> {
   float applyAsFloat(T value);
}
