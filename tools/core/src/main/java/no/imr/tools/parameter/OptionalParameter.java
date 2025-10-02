package no.imr.tools.parameter;

import no.marec.lsss.api.util.parameters.ValueConstraint;

import java.util.Optional;

public class OptionalParameter<T> extends ValueParameter<Optional<T>> {
   public OptionalParameter(Name name, Optional<T> initialValue, Unit unit, String description, ValueConverter<Optional<T>> converter, ValueConstraint<T> constraint) {
      super(name, initialValue, unit, constraint.forOptionalValues(), converter, description);
   }

   public OptionalParameter(Name name, Optional<T> initialValue, Unit unit, String description, ValueConverter<Optional<T>> converter) {
      super(name, initialValue, unit, converter, description);
   }

   public void setEmpty() {
      setValue(Optional.empty());
   }
}
