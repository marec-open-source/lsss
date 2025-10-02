package no.imr.korona.viewer.variables;

import no.imr.korona.data.ping.Ping;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;

/**
 * A continuous variable.
 */
public abstract non-sealed class ContinuousVariable extends BaseVariable {
   private final ContinuousVariableSettings settings;
   private final ExportTransform exportTransform;

   protected ContinuousVariable(VariableGroup variableGroup, Name name, ContinuousVariableSettings settings, Unit unit, ExportTransform exportTransform) {
      super(variableGroup, name, unit);

      this.settings = settings;
      this.exportTransform = exportTransform;
   }

   public ContinuousVariableSettings getSettings() {
      return settings;
   }

   public ExportTransform getExportTransform() {
      return exportTransform;
   }

   public abstract ContinuousVariableResult evaluate(int channel, Ping ping);
}
