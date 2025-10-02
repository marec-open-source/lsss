package no.marec.lsss.api;

import no.marec.lsss.api.config.DataConfig;
import no.marec.lsss.api.modules.ModuleRegistry;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A plugin for adding new functionality to LSSS.
 */
public interface LsssPlugin {
   /**
    * {@return the resource directory of optional help pages}
    */
   default @Nullable String getHelpResourceDir() {
      return null;
   }

   /**
    * Add the modules and overlays provided by this plugin.
    *
    * @param moduleRegistry where modules and overlays are added
    */
   default void addModules(ModuleRegistry moduleRegistry) {
   }

   /**
    * Called after all modules are created.
    */
   default void setup() {
   }

   /**
    * {@return data configurations provided by this plugin}
    */
   default List<DataConfig> getDataConfigs() {
      return List.of();
   }
}
