package no.imr.korona.util.schoolparameters;

import no.imr.tools.parameter.Name;

public class IntSchoolParameter extends SimpleSchoolParameter {
   private int value;

   public IntSchoolParameter(Name name, String unit) {
      super(name, unit);
   }

   public int getValue() {
      return value;
   }

   public void setValue(int value) {
      this.value = value;
   }

   @Override
   protected String getXmlValue() {
      return Integer.toString(value);
   }

   @Override
   protected void setXmlValue(String xmlValue) {
      value = Integer.parseInt(xmlValue);
   }
}
