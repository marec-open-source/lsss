package no.imr.tools.parameter;

import no.marec.lsss.api.util.parameters.ValueConstraint;

/**
 * Parameter for integer values.
 */
public class IntParameter extends ValueParameter<Integer> {
   public IntParameter(Name name, int initialValue, Unit unit) {
      this(name, initialValue, unit, "");
   }

   public IntParameter(Name name, int initialValue, Unit unit, String description) {
      super(name, initialValue, unit, ValueConverters.INTEGER, description);
   }

   public IntParameter(Name name, int initialValue, Unit unit, ValueConstraint<Integer> constraint) {
      this(name, initialValue, unit, constraint, "");
   }

   public IntParameter(Name name, int initialValue, Unit unit, ValueConstraint<Integer> constraint, String description) {
      super(name, initialValue, unit, constraint, ValueConverters.INTEGER, description);
   }

   public int getIntValue() {
      return getValue();
   }

   public void setIntValue(int value) {
      setValue(value);
   }

   public void add(int delta) {
      setIntValue(getIntValue() + delta);
   }
}
