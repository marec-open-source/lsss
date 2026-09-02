package no.imr.korona.util.schoolparameters;

import no.imr.tools.Utils;
import no.imr.tools.parameter.Name;

public class FloatSchoolParameter extends SimpleSchoolParameter {
   private final String format;
   private float value;

   public FloatSchoolParameter(Name name) {
      this(name, "", "%f");
   }

   public FloatSchoolParameter(Name name, String unit, String format) {
      super(name, unit);

      this.format = format;
   }

   @Override
   protected String getExportValue() {
      return Utils.format(format, value);
   }

   @Override
   protected String getXmlValue() {
      return Utils.toString(value);
   }

   @Override
   protected void setXmlValue(String xmlValue) {
      value = Float.parseFloat(xmlValue);
   }

   public float getValue() {
      return value;
   }

   public void setValue(float value) {
      this.value = value;
   }

   public void setValue(double value) {
      this.value = (float) value;
   }
}
