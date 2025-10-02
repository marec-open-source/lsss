package no.imr.korona.config;

import no.imr.korona.Korona;
import no.imr.tools.logging.Log;
import no.imr.tools.smoke.SmokeTestException;
import no.imr.tools.smoke.SmokeTestRunnable;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigFileSettingsSmoke extends SmokeTestRunnable {
   private final Korona korona;

   public ConfigFileSettingsSmoke(Korona korona) {
      this.korona = korona;
   }

   @Override
   public void run() {
      ConfigFileSettings configFileSettings = korona.createConfigFileSettings();
      for (ConfigFileService configFileService : configFileSettings.getFileServices()) {
         if (configFileService.getInstallationLocation() == null) {
            continue;
         }
         if (!Files.isDirectory(configFileService.getInstallationConfigDir())) {
            throw new SmokeTestException(configFileService + " " + configFileService.getInstallationConfigDir() + " is not a directory");
         }
         if (!Files.exists(configFileService.getInstallationLocation())) {
            throw new SmokeTestException(configFileService + " " + configFileService.getInstallationLocation() + " does not exist");
         }
         for (Path installationLocation : configFileService.getAdditionalInstallationLocations()) {
            if (!Files.exists(installationLocation)) {
               throw new SmokeTestException(configFileService + " " + installationLocation + " does not exist");
            }
         }
      }
      Log.global.info(OK + "ConfigFileSettings: " + configFileSettings.getFileServices());
   }
}
