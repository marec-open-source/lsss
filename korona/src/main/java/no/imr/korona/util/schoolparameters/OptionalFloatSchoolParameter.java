package no.imr.korona.util.schoolparameters;

import no.imr.tools.parameter.Name;

public final class OptionalFloatSchoolParameter extends FloatSchoolParameter {
   public OptionalFloatSchoolParameter(Name name, String unit, String format) {
      super(name, unit, format);

      setNotAvailable();
   }

   public boolean isAvailable() {
      return !Float.isNaN(getValue());
   }

   public void setNotAvailable() {
      setValue(Float.NaN);
   }

   @Override
   protected String getExportValue() {
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
