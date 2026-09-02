package no.imr.korona.viewer.variables.categorization;

import no.imr.korona.data.datagrams.Cad0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.viewer.variables.ContinuousVariableResult;
import no.imr.tools.parameter.Name;
import org.jspecify.annotations.Nullable;

/**
 * Probability.
 */
public final class ProbabilityVariable extends ContinuousCategorizationVariable {
   private final CategoryVariable categoryVariable;

   ProbabilityVariable(CategoryVariable categoryVariable) {
      super(new Name("probability", "Probability"), true);

      this.categoryVariable = categoryVariable;
   }

   @Override
   public @Nullable ContinuousVariableResult evaluate(int channel, Ping ping) {
      Cad0Datagram cad0Datagram = ping.getPingItem(Cad0Datagram.class);
      if (cad0Datagram == null) {
         return null;
      } else {
         return ContinuousVariableResult.of(cad0Datagram.getBestProbabilities(categoryVariable), cad0Datagram.getDepthRange());
      }
   }
}
