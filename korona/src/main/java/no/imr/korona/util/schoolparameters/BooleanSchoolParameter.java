package no.imr.korona.util.schoolparameters;

import no.imr.tools.parameter.Name;

public final class BooleanSchoolParameter extends SimpleSchoolParameter {
   private boolean value;

   public BooleanSchoolParameter(Name name) {
      super(name, "");
   }

   @Override
   public String getXmlValue() {
      return Boolean.toString(value);
   }

   @Override
   protected void setXmlValue(String xmlValue) {
      value = Boolean.parseBoolean(xmlValue);
   }

   public boolean getValue() {
      return value;
   }

   public void setValue(boolean value) {
      this.value = value;
   }
}
