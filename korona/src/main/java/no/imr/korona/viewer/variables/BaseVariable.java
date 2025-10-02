package no.imr.korona.viewer.variables;

import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;

import java.util.List;

/**
 * Base class for discrete and continuous variables.
 */
public abstract sealed class BaseVariable permits ContinuousVariable, DiscreteVariable {
   private final VariableGroup variableGroup;
   private final Name name;
   private final Unit unit;

   BaseVariable(VariableGroup variableGroup, Name name, Unit unit) {
      this.variableGroup = variableGroup;
      this.name = name;
      this.unit = unit;
   }

   @Override
   public String toString() {
      return name.persistentName();
   }

   public VariableGroup getVariableGroup() {
      return variableGroup;
   }

   public Name getName() {
      return name;
   }

   public String getDisplayName() {
      return name.displayName();
   }

   public Unit getUnit() {
      return unit;
   }

   public abstract boolean isUsableInContext();

   public void updateEvaluationContext(List<PingItem> configurationItems, ConfigFileSettings configFileSettings) {
   }
}
