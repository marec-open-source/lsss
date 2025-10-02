package no.imr.tools.parameter;

import java.util.Collection;

/**
 * A container of {@link Configurable}s.
 */
public final class ConfigurableContainer extends Configurable {
   private final Collection<? extends Configurable> subConfigurables;

   public ConfigurableContainer(Name name, Collection<? extends Configurable> subConfigurables) {
      super(name);

      this.subConfigurables = subConfigurables;
   }

   @Override
   public Collection<? extends Configurable> getSubConfigurables() {
      return subConfigurables;
   }
}
