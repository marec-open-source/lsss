package no.marec.lsss.api.util.parameters;

import no.marec.lsss.api.DoNotImplement;
import no.marec.lsss.api.util.observing.ObservableProperty;

/**
 * A configuration parameter.
 *
 * @param <T> the value type
 */
@DoNotImplement
public interface ConfigParameter<T> extends BaseConfigParameter<T>, ObservableProperty<T> {
   /**
    * Set a constraint on the values accepted by this parameter.
    *
    * @param constraint a value constraint
    * @return this parameter
    * @throws IllegalArgumentException if the current value is not compatible with the new constraint
    */
   ConfigParameter<T> setConstraint(ValueConstraint<T> constraint);
}
