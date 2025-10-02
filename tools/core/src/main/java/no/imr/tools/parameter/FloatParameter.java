package no.imr.tools.parameter;

import no.marec.lsss.api.util.parameters.ValueConstraint;
import no.marec.lsss.api.util.parameters.ValueConstraints;

/**
 * Parameter for float values.
 */
public class FloatParameter extends ValueParameter<Float> {
   public FloatParameter(Name name, float initialValue, Unit unit) {
      this(name, initialValue, unit, "");
   }

   public FloatParameter(Name name, float initialValue, Unit unit, String description) {
      super(name, initialValue, unit, ValueConverters.FLOAT, description);
   }

   public FloatParameter(Name name, float initialValue, Unit unit, ValueConstraint<Float> constraint) {
      this(name, initialValue, unit, constraint, "");
   }

   public FloatParameter(Name name, float initialValue, Unit unit, ValueConstraint<Float> constraint, String description) {
      super(name, initialValue, unit, constraint, ValueConverters.FLOAT, description);
   }

   public float getFloatValue() {
      return getValue();
   }

   public void setFloatValue(float value) {
      setValue(value);
   }

   public void setAtLeastTo(float minValue) {
      if (getFloatValue() < minValue) {
         setFloatValue(minValue);
      }
   }

   public void setAtMostTo(float maxValue) {
      if (getFloatValue() > maxValue) {
         setFloatValue(maxValue);
      }
   }

   /**
    * Changes the minimum and maximum values this parameter can take.
    *
    * @param min the new minimum value
    * @param max the new maximum value
    * @throws IllegalArgumentException if min &gt; max
    */
   public void setRange(float min, float max) {
      setConstraintAndPossiblyValue(ValueConstraints.gteLte(min, max), Math.clamp(getFloatValue(), min, max));
   }
}
