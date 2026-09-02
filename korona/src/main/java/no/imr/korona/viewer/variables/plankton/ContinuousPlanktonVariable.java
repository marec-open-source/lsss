package no.imr.korona.viewer.variables.plankton;

import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.datagrams.Pic0Datagram;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.tools.Utils;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Continuous variables available from plankton inversion.
 */
abstract class ContinuousPlanktonVariable extends ContinuousVariable {
   private @Nullable Pic0Datagram pic0Datagram;

   ContinuousPlanktonVariable(Name name, ContinuousVariableSettings settings, Unit unit, ExportTransform exportTransform) {
      super(PlanktonVariableFactory.PLANKTON_VARIABLE_GROUP, name, settings, unit, exportTransform);
   }

   @Override
   public boolean isUsableInContext() {
      return pic0Datagram != null;
   }

   @Override
   public void updateEvaluationContext(List<PingItem> configurationItems, ConfigFileSettings configFileSettings) {
      pic0Datagram = Utils.getFirstOrNull(configurationItems, Pic0Datagram.class);
   }

   @Nullable Pic0Datagram getPic0Datagram() {
      return pic0Datagram;
   }
}
