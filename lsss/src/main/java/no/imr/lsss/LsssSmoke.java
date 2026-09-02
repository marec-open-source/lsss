package no.imr.lsss;

import no.imr.korona.computation.ModuleManagerSmoke;
import no.imr.korona.config.ConfigFileSettingsSmoke;
import no.imr.lsss.database.types.TestDatabasePlugin;
import no.imr.lsss.framework.LsssConfig;
import no.imr.lsss.framework.ServiceCollection;
import no.imr.lsss.modules.interpretation.FrequencyResponseFunctionParameterSmoke;
import no.imr.tools.help.HelpDisplayerService;
import no.imr.tools.logging.Log;
import no.imr.tools.smoke.SmokeTestException;
import no.imr.tools.smoke.SmokeTestExecutor;
import no.imr.tools.smoke.SmokeTestRunnable;
import no.imr.tools.smoke.ToolsSmoke;

import java.util.ServiceLoader;

/**
 * LSSS smoke test.
 */
final class LsssSmoke extends SmokeTestRunnable {
   private LsssSmoke() {
   }

   @Override
   public void run() throws Exception {
      LsssConfig lsssConfig = new LsssConfig(new ServiceCollection()).skipLoadSetting();
      LSSS lsss = new LSSS(lsssConfig);
      TestDatabasePlugin.install(lsss);
      lsssConfig.onClose = LSSS.LOGGING_MANAGER::shutDown;
      assert ServiceLoader.load(HelpDisplayerService.class).findFirst().isPresent();
      new ConfigFileSettingsSmoke(lsss.getKorona()).run();
      new ModuleManagerSmoke(lsss.getKorona().getModuleManager()).run();
      new FrequencyResponseFunctionParameterSmoke().run();
      lsss.getConfigurationManager().getSurveyConfiguration().getPreprocessingConf().getMainSetup().smokeTest();
      testPlugins(lsss);
      testDatabaseConnection(lsss);
      Log.global.info(OK + "LSSS");
      lsss.close();
   }

   private static void testPlugins(LSSS lsss) {
      if (lsss.getPluginManager().getFeaturePlugins().isEmpty()) {
         throw new SmokeTestException("No feature plugins");
      }
      Log.global.info(OK + "Feature plugins: " + lsss.getPluginManager().getFeaturePlugins());
   }

   private static void testDatabaseConnection(LSSS lsss) {
      lsss.getDatabaseManager().getConnectionManager().initializeDatabase();
      if (!lsss.getDatabaseManager().getDatabaseConnection().isConnected()) {
         throw new SmokeTestException("Not connected to database");
      }
      Log.global.info(OK + "Database connection");
   }

   static void main() {
      SmokeTestExecutor.execute(LSSS.LOGGING_MANAGER, new ToolsSmoke(), new LsssSmoke()); // Does not work on Linux Jenkins: new JoglSmoke()
   }
}
