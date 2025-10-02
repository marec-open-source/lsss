package no.marec.lsss.api.util.parameters;

import no.marec.lsss.api.DoNotImplement;

/**
 * Converts values to string.
 *
 * @param <T> the value type
 */
@DoNotImplement
@FunctionalInterface
public interface ValueStringifier<T> {
   /**
    * {@return the string representation of a value}
    *
    * @param value a value
    */
   String stringify(T value);
}
