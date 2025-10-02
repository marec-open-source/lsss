package no.imr.korona.viewer.variables.raw;

import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;

/**
 * Continuous variables available from {@link PowerData}.
 */
abstract class ContinuousRawVariable extends ContinuousVariable {
   ContinuousRawVariable(Name name, ContinuousVariableSettings settings, Unit unit, ExportTransform exportTransform) {
      super(RawVariableFactory.RAW_VARIABLE_GROUP, name, settings, unit, exportTransform);
   }

   @Override
   public boolean isUsableInContext() {
      return true;
   }
}
