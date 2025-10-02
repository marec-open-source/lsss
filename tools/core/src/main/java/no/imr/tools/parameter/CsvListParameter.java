package no.imr.tools.parameter;

import no.marec.lsss.api.util.parameters.ValueConstraint;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.util.List;

/**
 * A comma separated list of values.
 *
 * @param <T> the value type
 */
public class CsvListParameter<T> extends ValueParameter<List<T>> {
   public CsvListParameter(Name name, List<T> initialValue, Unit unit, ValueConstraint<T> constraint, ValueConverter<T> converter, String description) {
      super(name, initialValue, unit, ValueConstraints.listItem(constraint, converter), ValueConverters.csvList(converter), description);
   }

   public CsvListParameter(Name name, List<T> initialValue, Unit unit, ValueConverter<T> converter, String description) {
      super(name, initialValue, unit, ValueConverters.csvList(converter), description);
   }
}
