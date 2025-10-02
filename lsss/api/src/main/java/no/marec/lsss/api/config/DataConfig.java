package no.marec.lsss.api.config;

import no.marec.lsss.api.util.parameters.BaseConfigParameter;

import java.util.List;

/**
 * Configuration unit for data used by an LSSS plugin.
 */
public interface DataConfig {
   /**
    * {@return the id of this configuration unit}
    */
   String getId();

   /**
    * {@return the display label of this configuration unit}
    */
   default String getLabel() {
      return getId();
   }

   /**
    * {@return a description of this configuration unit}
    */
   String getDescription();

   /**
    * {@return the configuration parameters of this configuration unit}
    */
   List<? extends BaseConfigParameter<?>> getParameters();
}
