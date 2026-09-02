package no.imr.korona.viewer.variables.categorization;

import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.datagrams.Cad0Datagram;
import no.imr.korona.data.datagrams.DiscreteCategory;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.viewer.variables.DiscreteVariable;
import no.imr.korona.viewer.variables.DiscreteVariableResult;
import no.imr.tools.LateInit;
import no.imr.tools.Utils;
import no.imr.tools.parameter.Name;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Category.
 */
public final class CategoryVariable extends DiscreteVariable {
   private @Nullable Cac0Datagram cac0Datagram;
   private Cac0Datagram.@Nullable Category unknownCategory;
   private final LateInit<ProbabilityVariable> probabilityVariable = new LateInit<>();

   public CategoryVariable() {
      super(CategorizationVariableFactory.CATEGORIZATION_VARIABLE_GROUP, new Name("category", "Category"));
   }

   void setProbabilityVariable(ProbabilityVariable probabilityVariable) {
      this.probabilityVariable.init(probabilityVariable);
   }

   public float getProbabilityThreshold() {
      return probabilityVariable.get().getSettings().getRange().min();
   }

   @Override
   public boolean isUsableInContext() {
      return cac0Datagram != null;
   }

   @Override
   public void updateEvaluationContext(List<PingItem> configurationItems, ConfigFileSettings configFileSettings) {
      cac0Datagram = Utils.getFirstOrNull(configurationItems, Cac0Datagram.class);
      if (cac0Datagram == null) {
         unknownCategory = null;
         getSettings().update(List.of());
      } else {
         unknownCategory = cac0Datagram.getUnknownCategory();
         getSettings().update(cac0Datagram.getCategories());
      }
   }

   @Override
   public DiscreteCategory getUnknownCategory() {
      if (unknownCategory == null) {
         throw new IllegalStateException();
      }
      return unknownCategory;
   }

   @Override
   public @Nullable DiscreteVariableResult evaluate(Ping ping) {
      Cac0Datagram cac0Datagram = this.cac0Datagram;
      Cad0Datagram cad0Datagram = ping.getPingItem(Cad0Datagram.class);
      if (cac0Datagram == null || cad0Datagram == null) {
         return null;
      }

      byte unknownCategory = cac0Datagram.getUnknownCategory().getNumber();
      byte[] byteData = cad0Datagram.getBestCategories(this, unknownCategory);
      return DiscreteVariableResult.of(byteData, cad0Datagram.getDepthRange());
   }
}
