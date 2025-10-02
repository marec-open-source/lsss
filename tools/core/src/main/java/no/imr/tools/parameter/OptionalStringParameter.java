package no.imr.tools.parameter;

import no.marec.lsss.api.util.parameters.ValueConstraint;

import java.util.Optional;

public class OptionalStringParameter extends OptionalParameter<String> {
   public OptionalStringParameter(Name name, Optional<String> initialValue, String description) {
      super(name, initialValue, Unit.NONE, description, ValueConverters.OPTIONAL_STRING);
   }

   public OptionalStringParameter(Name name, Optional<String> initialValue, ValueConstraint<String> constraint, String description) {
      super(name, initialValue, Unit.NONE, description, ValueConverters.OPTIONAL_STRING, constraint);
   }
}
