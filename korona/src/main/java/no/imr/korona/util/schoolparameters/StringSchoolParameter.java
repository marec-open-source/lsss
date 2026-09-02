package no.imr.korona.util.schoolparameters;

import no.imr.tools.parameter.Name;

public class StringSchoolParameter extends SimpleSchoolParameter {
   private String value = "";

   public StringSchoolParameter(Name name) {
      this(name, "");
   }

   public StringSchoolParameter(Name name, String unit) {
      super(name, unit);
   }

   public void setValue(String value) {
      this.value = value;
   }

   @Override
   protected String getXmlValue() {
      return value;
   }

   @Override
   protected void setXmlValue(String xmlValue) {
      value = xmlValue;
   }
}
