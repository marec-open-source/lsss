package no.imr.lsss.database.ices;

import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.ValueParameter;

import java.util.Collection;
import java.util.Set;

public abstract class IcesGroup extends Configurable implements ParameterContainer {
   IcesGroup(Name name) {
      super(name);
   }

   public abstract Set<ValueParameter<?>> getMandatoryParameters();

   @Override
   public Collection<? extends Configurable> getSubConfigurables() {
      return getParameters();
   }
}
