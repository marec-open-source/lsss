package no.imr.korona.viewer.variables.categorization;

import no.imr.korona.data.datagrams.Cad0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.viewer.variables.ContinuousVariableResult;
import no.imr.tools.parameter.Name;

/**
 * Discriminant.
 */
public final class DiscriminantVariable extends ContinuousCategorizationVariable {
   private final CategoryVariable categoryVariable;

   DiscriminantVariable(CategoryVariable categoryVariable) {
      super(new Name("discriminant", "Discriminant"), true);

      this.categoryVariable = categoryVariable;
   }

   @Override
   public ContinuousVariableResult evaluate(int channel, Ping ping) {
      Cad0Datagram cad0Datagram = ping.getPingItem(Cad0Datagram.class);
      if (cad0Datagram == null) {
         return ContinuousVariableResult.EMPTY;
      } else {
         return new ContinuousVariableResult(cad0Datagram.getBestDiscriminants(categoryVariable), cad0Datagram.getDepthRange());
      }
   }
}
