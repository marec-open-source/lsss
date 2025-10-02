package no.imr.korona.computation.plugin;

import no.imr.tools.compile.CompileException;
import no.imr.tools.logging.Log;
import no.imr.tools.smoke.SmokeTestRunnable;

public final class PluginModuleSmoke extends SmokeTestRunnable {
   public PluginModuleSmoke() {
   }

   @Override
   public void run() throws CompileException {
      for (Example example : Example.EXAMPLES) {
         PluginModule.compile(example.getImplementation());
      }
      Log.global.info(OK + "PluginModule: " + Example.EXAMPLES.size());
   }
}
