package no.imr.korona.util.schoolparameters;

import no.imr.tools.parameter.Name;

public abstract class OptionalIntSchoolParameter extends IntSchoolParameter {
   protected OptionalIntSchoolParameter(Name name, String unit) {
      super(name, unit);
   }

   public abstract boolean isAvailable();

   public abstract void setNotAvailable();

   @Override
   public String getExportValue() {
      return isAvailable() ? super.getExportValue() : NOT_AVAILABLE;
   }

   @Override
   protected String getXmlValue() {
      return isAvailable() ? super.getXmlValue() : NOT_AVAILABLE;
   }

   @Override
   protected void setXmlValue(String xmlValue) {
      if (xmlValue.equals(NOT_AVAILABLE)) {
         setNotAvailable();
      } else {
         super.setXmlValue(xmlValue);
      }
   }
}
