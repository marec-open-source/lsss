package no.imr.tools.parameter;

/**
 * Parameter for boolean values.
 */
public class BooleanParameter extends ValueParameter<Boolean> {
   public BooleanParameter(Name name, boolean initialValue) {
      this(name, initialValue, "");
   }

   public BooleanParameter(Name name, boolean initialValue, String description) {
      this(name, initialValue, Unit.NONE, description);
   }

   public BooleanParameter(Name name, boolean initialValue, Unit unit, String description) {
      super(name, initialValue, unit, ValueConverters.BOOLEAN, description);
   }

   public boolean getBooleanValue() {
      return getValue();
   }

   public void setBooleanValue(boolean value) {
      setValue(value);
   }

   public void setFalse() {
      setBooleanValue(false);
   }

   public void setTrue() {
      setBooleanValue(true);
   }

   public void toggle() {
      setBooleanValue(!getBooleanValue());
   }
}
