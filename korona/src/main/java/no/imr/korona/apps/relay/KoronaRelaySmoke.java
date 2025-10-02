package no.imr.korona.apps.relay;

import no.imr.tools.logging.Log;
import no.imr.tools.smoke.SmokeTestExecutor;
import no.imr.tools.smoke.SwingSmokeTestRunnable;
import no.imr.tools.smoke.ToolsSmoke;

import java.util.Map;

/**
 * KoronaRelay smoke test.
 */
final class KoronaRelaySmoke extends SwingSmokeTestRunnable {
   private KoronaRelaySmoke() {
   }

   @Override
   public void swingRun() {
      KoronaRelay koronaRelay = new KoronaRelay(Map.of());
      Log.global.info(OK + "KoronaRelay");
      koronaRelay.shutDown();
   }

   public static void main(String[] args) {
      SmokeTestExecutor.execute(KoronaRelay.LOGGING_MANAGER, new ToolsSmoke(), new KoronaRelaySmoke());
   }
}
