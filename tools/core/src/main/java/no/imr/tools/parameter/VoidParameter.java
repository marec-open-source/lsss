package no.imr.tools.parameter;

import no.marec.lsss.api.util.parameters.VoidConfigParameter;

import java.util.Optional;

/**
 * Base class for parameters that don't have values.
 */
public abstract sealed class VoidParameter extends BaseParameter<Optional<Void>> implements VoidConfigParameter
      permits ButtonParameter, CustomGuiParameter, HeaderParameter, SeparatorParameter {

   VoidParameter(Name name, Unit unit, String description) {
      super(name, unit, description);
   }

   @Override
   public boolean isPersistable() {
      return false;
   }

   @Override
   Optional<Void> getValue() {
      return Optional.empty();
   }
}
