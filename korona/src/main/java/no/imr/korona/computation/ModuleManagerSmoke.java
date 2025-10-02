package no.imr.korona.computation;

import no.imr.korona.util.ChannelPredicateParameterSmoke;
import no.imr.tools.logging.Log;
import no.imr.tools.smoke.SmokeTestException;
import no.imr.tools.smoke.SmokeTestRunnable;

public final class ModuleManagerSmoke extends SmokeTestRunnable {
   private final ModuleManager moduleManager;

   public ModuleManagerSmoke(ModuleManager moduleManager) {
      this.moduleManager = moduleManager;
   }

   @Override
   public void run() throws Exception {
      if (moduleManager.getModuleInfos().isEmpty()) {
         throw new SmokeTestException("No modules");
      }
      for (ModuleInfo moduleInfo : moduleManager.getModuleInfos()) {
         if (moduleInfo.isDeprecated()) {
            continue;
         }
         BaseModule module = moduleManager.createModule(moduleInfo.getPersistentName());
         module.runSmokeTest();
      }
      Log.global.info(OK + "ModuleManager: "
            + moduleManager.getModuleInfos().size() + " modules, "
            + moduleManager.getModulePlugins().size() + " plugins: " + moduleManager.getModulePlugins());

      new ChannelPredicateParameterSmoke().run();
   }
}
