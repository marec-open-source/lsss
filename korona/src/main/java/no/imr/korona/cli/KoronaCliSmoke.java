package no.imr.korona.cli;

import no.imr.tools.logging.Log;
import no.imr.tools.smoke.SmokeTestExecutor;
import no.imr.tools.smoke.SmokeTestRunnable;
import no.imr.tools.smoke.ToolsSmoke;

import java.util.List;

final class KoronaCliSmoke extends SmokeTestRunnable {
   private KoronaCliSmoke() {
   }

   @Override
   public void run() {
      List<CliCommandInfo> infos = CliCommandFactory.infos();
      for (CliCommandInfo info : infos) {
         CliCommandFactory.create(info.name());
      }
      Log.global.info(OK + "KoronaCli: "
            + infos.size() + " commands: " + infos.stream().map(CliCommandInfo::name).toList());
      KoronaCli.LOGGING_MANAGER.shutDown();
   }

   static void main() {
      SmokeTestExecutor.execute(KoronaCli.LOGGING_MANAGER, new ToolsSmoke(), new KoronaCliSmoke());
   }
}
