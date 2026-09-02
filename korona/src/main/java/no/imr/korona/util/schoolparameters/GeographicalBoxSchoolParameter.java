package no.imr.korona.util.schoolparameters;

import no.imr.tools.parameter.Name;

import java.util.List;

public final class GeographicalBoxSchoolParameter extends RectangleSchoolParameter {
   public GeographicalBoxSchoolParameter(Name name, String unit, String format) {
      super(name, unit, format);
   }

   @Override
   public List<String> getExportNames() {
      String name = getName().persistentName();
      return List.of(
            name + ".lon.min",
            name + ".lat.min",
            name + ".lon.max",
            name + ".lat.max"
      );
   }
}
