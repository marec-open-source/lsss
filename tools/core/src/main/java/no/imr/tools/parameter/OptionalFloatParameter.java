package no.imr.tools.parameter;

import no.marec.lsss.api.util.parameters.ValueConstraint;

import java.util.Optional;

/**
 * Parameter for optional float values.
 */
public class OptionalFloatParameter extends OptionalParameter<Float> {
   public OptionalFloatParameter(Name name, Optional<Float> initialValue, Unit unit) {
      this(name, initialValue, unit, "");
   }

   public OptionalFloatParameter(Name name, Optional<Float> initialValue, Unit unit, String description) {
      super(name, initialValue, unit, description, ValueConverters.OPTIONAL_FLOAT);
   }

   public OptionalFloatParameter(Name name, Optional<Float> initialValue, Unit unit, ValueConstraint<Float> constraint) {
      this(name, initialValue, unit, constraint, "");
   }

   public OptionalFloatParameter(Name name, Optional<Float> initialValue, Unit unit, ValueConstraint<Float> constraint, String description) {
      super(name, initialValue, unit, description, ValueConverters.OPTIONAL_FLOAT, constraint);
   }

   public void setFloatValue(float value) {
      setValue(Optional.of(value));
   }
}
