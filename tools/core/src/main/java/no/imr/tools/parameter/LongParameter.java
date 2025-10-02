package no.imr.tools.parameter;

import no.marec.lsss.api.util.parameters.ValueConstraint;

public class LongParameter extends ValueParameter<Long> {
   public LongParameter(Name name, long initialValue, Unit unit) {
      this(name, initialValue, unit, "");
   }

   public LongParameter(Name name, long initialValue, Unit unit, String description) {
      super(name, initialValue, unit, ValueConverters.LONG, description);
   }

   public LongParameter(Name name, long initialValue, Unit unit, ValueConstraint<Long> constraint, String description) {
      super(name, initialValue, unit, constraint, ValueConverters.LONG, description);
   }

   public long getLongValue() {
      return getValue();
   }

   public void setLongValue(long value) {
      setValue(value);
   }

   public void add(long delta) {
      setLongValue(getLongValue() + delta);
   }
}
