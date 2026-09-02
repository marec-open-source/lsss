package no.imr.tools.plugins;

import no.imr.tools.parameter.Name;

/**
 * The plugin created by a {@link BaseService}.
 */
public abstract class BasePlugin {
   private final Name name;

   protected BasePlugin(Name name) {
      this.name = name;
   }

   public Name getName() {
      return name;
   }

   public String getPersistentName() {
      return name.persistentName();
   }

   @Override
   public String toString() {
      return getPersistentName();
   }
}
