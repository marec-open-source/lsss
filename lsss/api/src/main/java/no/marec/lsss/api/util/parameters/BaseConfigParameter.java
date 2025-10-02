package no.marec.lsss.api.util.parameters;

import no.marec.lsss.api.DoNotImplement;
import no.marec.lsss.api.util.observing.Observable;

/**
 * Base type for configuration parameters.
 *
 * @param <T> the value type
 */
@DoNotImplement
public interface BaseConfigParameter<T> extends Observable<T> {
}
