package no.imr.korona.util.schoolparameters;

import no.imr.tools.Utils;
import no.imr.tools.parameter.Name;

abstract class BaseFloatSchoolParameter extends BaseSchoolParameter {
   private final String format;

   BaseFloatSchoolParameter(Name name, String unit, String format) {
      super(name, unit);

      this.format = format;
   }

   protected String format(double value) {
      return Utils.format(format, value);
   }
}
