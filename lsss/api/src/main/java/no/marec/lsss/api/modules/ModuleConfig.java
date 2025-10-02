package no.marec.lsss.api.modules;

import no.marec.lsss.api.util.parameters.BaseConfigParameter;

import java.util.List;

/**
 * The configuration of a {@link LsssModule}.
 */
public interface ModuleConfig {
   /**
    * {@return the configuration parameters of the module}
    */
   List<? extends BaseConfigParameter<?>> getParameters();
}
