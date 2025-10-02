package no.imr.korona.viewer.variables.categorization;

import no.imr.korona.data.datagrams.Cad0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.viewer.variables.ContinuousVariableResult;
import no.imr.tools.parameter.Name;

/**
 * Doubt.
 */
public final class DoubtVariable extends ContinuousCategorizationVariable {
   DoubtVariable() {
      super(new Name("doubt", "Doubt"), true);
   }

   @Override
   public ContinuousVariableResult evaluate(int channel, Ping ping) {
      Cad0Datagram cad0Datagram = ping.getPingItem(Cad0Datagram.class);
      if (cad0Datagram == null) {
         return ContinuousVariableResult.EMPTY;
      } else {
         return new ContinuousVariableResult(cad0Datagram.getDiscriminantDifference(), cad0Datagram.getDepthRange());
      }
   }
}
