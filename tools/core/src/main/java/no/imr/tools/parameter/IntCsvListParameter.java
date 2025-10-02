package no.imr.tools.parameter;

import no.marec.lsss.api.util.parameters.ValueConstraint;

import java.util.List;

public class IntCsvListParameter extends CsvListParameter<Integer> {
   public IntCsvListParameter(Name name, List<Integer> initialValue, Unit unit) {
      this(name, initialValue, unit, "");
   }

   public IntCsvListParameter(Name name, List<Integer> initialValue, Unit unit, String description) {
      super(name, initialValue, unit, ValueConverters.INTEGER, description);
   }

   public IntCsvListParameter(Name name, List<Integer> initialValue, Unit unit, ValueConstraint<Integer> constraint, String description) {
      super(name, initialValue, unit, constraint, ValueConverters.INTEGER, description);
   }
}
