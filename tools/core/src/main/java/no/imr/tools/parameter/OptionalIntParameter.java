package no.imr.tools.parameter;

import no.marec.lsss.api.util.parameters.ValueConstraint;

import java.util.Optional;

/**
 * Parameter for optional integer values.
 */
public class OptionalIntParameter extends OptionalParameter<Integer> {
   public OptionalIntParameter(Name name, Optional<Integer> initialValue, Unit unit) {
      this(name, initialValue, unit, "");
   }

   public OptionalIntParameter(Name name, Optional<Integer> initialValue, Unit unit, String description) {
      super(name, initialValue, unit, description, ValueConverters.OPTIONAL_INTEGER);
   }

   public OptionalIntParameter(Name name, Optional<Integer> initialValue, Unit unit, ValueConstraint<Integer> constraint) {
      this(name, initialValue, unit, constraint, "");
   }

   public OptionalIntParameter(Name name, Optional<Integer> initialValue, Unit unit, ValueConstraint<Integer> constraint, String description) {
      super(name, initialValue, unit, description, ValueConverters.OPTIONAL_INTEGER, constraint);
   }

   public void setIntValue(int value) {
      setValue(Optional.of(value));
   }
}
