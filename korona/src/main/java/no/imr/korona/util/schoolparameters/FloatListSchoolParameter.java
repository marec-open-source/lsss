package no.imr.korona.util.schoolparameters;

import no.imr.tools.parameter.Name;

public final class FloatListSchoolParameter extends ListSchoolParameter<FloatSchoolParameter> {
   public FloatListSchoolParameter(Name name, String unit, String format) {
      super(name, unit, format);
   }

   @Override
   protected FloatSchoolParameter createNewParameter(Name name, String unit, String format) {
      return new FloatSchoolParameter(name, unit, format);
   }
}
