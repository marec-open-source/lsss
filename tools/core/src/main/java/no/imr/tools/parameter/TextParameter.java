package no.imr.tools.parameter;

import no.marec.lsss.api.util.parameters.ValueConstraint;
import no.marec.lsss.api.util.parameters.ValueConstraints;

/**
 * Parameter for text.
 */
public class TextParameter extends ValueParameter<String> {
   public TextParameter(Name name) {
      this(name, "", "");
   }

   public TextParameter(Name name, String initialValue) {
      this(name, initialValue, "");
   }

   public TextParameter(Name name, String initialValue, String description) {
      this(name, initialValue, ValueConstraints.none(), description);
   }

   public TextParameter(Name name, String initialValue, ValueConstraint<String> constraint) {
      this(name, initialValue, constraint, "");
   }

   public TextParameter(Name name, String initialValue, ValueConstraint<String> constraint, String description) {
      super(name, initialValue, Unit.NONE, constraint, ValueConverters.STRING, description);
   }
}
