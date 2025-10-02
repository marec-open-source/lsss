package no.imr.korona.computation.plugin;

import no.marec.api.korona.Ping;
import no.marec.api.korona.SinglePingModuleComputation;

final class EmptyPluginComputation extends SinglePingModuleComputation {
   EmptyPluginComputation() {
   }

   @Override
   public void compute(Ping ping) {
   }
}
