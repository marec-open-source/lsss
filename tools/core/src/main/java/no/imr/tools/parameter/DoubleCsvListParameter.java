package no.imr.tools.parameter;

import java.util.List;

public class DoubleCsvListParameter extends CsvListParameter<Double> {
   public DoubleCsvListParameter(Name name, List<Double> initialValue, Unit unit, String description) {
      super(name, initialValue, unit, ValueConverters.DOUBLE, description);
   }
}
