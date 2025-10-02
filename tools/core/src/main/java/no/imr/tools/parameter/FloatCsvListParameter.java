package no.imr.tools.parameter;

import java.util.List;

public class FloatCsvListParameter extends CsvListParameter<Float> {
   public FloatCsvListParameter(Name name, List<Float> initialValue, Unit unit, String description) {
      super(name, initialValue, unit, ValueConverters.FLOAT, description);
   }
}
