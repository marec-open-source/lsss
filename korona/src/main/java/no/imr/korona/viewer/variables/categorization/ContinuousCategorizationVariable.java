package no.imr.korona.viewer.variables.categorization;

import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.tools.Utils;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Continuous variables available from categorization.
 */
abstract class ContinuousCategorizationVariable extends ContinuousVariable {
   private @Nullable Cac0Datagram cac0Datagram;

   ContinuousCategorizationVariable(Name name, boolean proportional) {
      super(CategorizationVariableFactory.CATEGORIZATION_VARIABLE_GROUP, name,
            new ContinuousVariableSettings(FloatRange.of(0, 1), FloatRange.of(0.1f, 0.9f), 0.01, proportional),
            Unit.NONE, ExportTransform.round(10_000));
   }

   @Override
   public boolean isUsableInContext() {
      return cac0Datagram != null;
   }

   @Override
   public void updateEvaluationContext(List<PingItem> configurationItems, ConfigFileSettings configFileSettings) {
      cac0Datagram = Utils.getFirstOrNull(configurationItems, Cac0Datagram.class);
   }
}
