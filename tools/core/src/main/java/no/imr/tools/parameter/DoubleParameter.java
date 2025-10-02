package no.imr.tools.parameter;

import no.marec.lsss.api.util.parameters.ValueConstraint;

/**
 * Parameter for double values.
 */
public class DoubleParameter extends ValueParameter<Double> {
   public DoubleParameter(Name name, double initialValue, Unit unit) {
      this(name, initialValue, unit, "");
   }

   public DoubleParameter(Name name, double initialValue, Unit unit, String description) {
      super(name, initialValue, unit, ValueConverters.DOUBLE, description);
   }

   public DoubleParameter(Name name, double initialValue, Unit unit, ValueConstraint<Double> constraint) {
      this(name, initialValue, unit, constraint, "");
   }

   public DoubleParameter(Name name, double initialValue, Unit unit, ValueConstraint<Double> constraint, String description) {
      super(name, initialValue, unit, constraint, ValueConverters.DOUBLE, description);
   }

   public double getDoubleValue() {
      return getValue();
   }

   public void setDoubleValue(double value) {
      setValue(value);
   }
}
