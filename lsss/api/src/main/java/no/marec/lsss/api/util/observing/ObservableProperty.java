package no.marec.lsss.api.util.observing;

import no.marec.lsss.api.DoNotImplement;

/**
 * A mutable value container.
 *
 * @param <T> the value type
 */
@DoNotImplement
public interface ObservableProperty<T> extends ObservableValue<T> {
   /**
    * Sets the value.
    *
    * @param value the new value.
    */
   void setValue(T value);
}
