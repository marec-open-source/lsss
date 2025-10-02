package no.imr.tools.parameter;

import no.marec.lsss.api.util.parameters.ValueConstraint;
import no.marec.lsss.api.util.parameters.ValueConstraints;

/**
 * Parameter for strings.
 */
public class StringParameter extends ValueParameter<String> {
   public StringParameter(Name name) {
      this(name, "", "");
   }

   public StringParameter(Name name, String initialValue) {
      this(name, initialValue, "");
   }

   public StringParameter(Name name, String initialValue, String description) {
      this(name, initialValue, ValueConstraints.none(), description);
   }

   public StringParameter(Name name, String initialValue, ValueConstraint<String> constraint) {
      this(name, initialValue, constraint, "");
   }

   public StringParameter(Name name, String initialValue, ValueConstraint<String> constraint, String description) {
      super(name, initialValue, Unit.NONE, constraint, ValueConverters.STRING, description);
   }
}
