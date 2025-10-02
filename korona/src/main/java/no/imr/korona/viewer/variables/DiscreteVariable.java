package no.imr.korona.viewer.variables;

import no.imr.korona.data.datagrams.DiscreteCategory;
import no.imr.korona.data.ping.Ping;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;

/**
 * A discrete variable.
 */
public abstract non-sealed class DiscreteVariable extends BaseVariable {
   private final DiscreteVariableSettings settings = new DiscreteVariableSettings();

   protected DiscreteVariable(VariableGroup variableGroup, Name name) {
      super(variableGroup, name, Unit.NONE);
   }

   public DiscreteVariableSettings getSettings() {
      return settings;
   }

   public abstract DiscreteCategory getUnknownCategory();

   public abstract DiscreteVariableResult evaluate(Ping ping);
}
