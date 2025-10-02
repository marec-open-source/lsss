package no.marec.lsss.api.util.parameters;

import no.marec.lsss.api.DoNotImplement;

import java.util.Optional;

/**
 * Configuration parameters without a value.
 */
@DoNotImplement
public interface VoidConfigParameter extends BaseConfigParameter<Optional<Void>> {
}
