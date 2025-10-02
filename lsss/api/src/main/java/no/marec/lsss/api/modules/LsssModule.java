package no.marec.lsss.api.modules;

import no.marec.lsss.api.util.observing.SubscriptionRegistry;
import org.jspecify.annotations.Nullable;

/**
 * The base type for modules provided by an LSSS plugin.
 */
public interface LsssModule {
   /**
    * {@return the configuration of this module}
    */
   default @Nullable ModuleConfig getConfig() {
      return null;
   }

   /**
    * Called when the module becomes enabled.
    *
    * @param registry subscriptions added to this registry will automatically
    *                 be unsubscribed when this module is disabled
    */
   default void onEnable(SubscriptionRegistry registry) {
   }

   /**
    * Called when the module becomes disabled.
    */
   default void onDisable() {
   }
}
