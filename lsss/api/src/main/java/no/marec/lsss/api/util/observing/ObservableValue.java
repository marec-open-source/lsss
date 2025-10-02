package no.marec.lsss.api.util.observing;

import no.marec.lsss.api.DoNotImplement;

/**
 * A value container that emits notifications when the value changes.
 *
 * @param <T> the value type
 */
@DoNotImplement
public interface ObservableValue<T> extends Observable<T> {
   /**
    * {@return the current value}
    */
   T getValue();
}
