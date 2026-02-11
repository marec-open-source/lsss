package no.imr.korona.config.gui;

import no.imr.korona.Korona;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.KoronaConfigFileService;
import no.imr.korona.resources.KoronaHelp;
import no.imr.korona.resources.KoronaResource;
import no.imr.korona.viewer.KoronaHelpSystem;
import no.imr.tools.Utils;
import no.imr.tools.help.HelpSystem;

import javax.swing.SwingUtilities;

final class ConfigFileSettingsEditorMain {
   private ConfigFileSettingsEditorMain() {
   }

   static void main(String[] args) {
      Utils.init(args, KoronaResource.KORONA_64);
      SwingUtilities.invokeLater(ConfigFileSettingsEditorMain::start);
   }

   private static void start() {
      Korona korona = new Korona();
      HelpSystem helpSystem = KoronaHelpSystem.createHelpSystem(korona);
      ConfigFileSettings configFileSettings = korona.createConfigFileSettings();
      configFileSettings.setContext(KoronaConfigFileService.CONTEXT);
      configFileSettings.restoreInstallationLocations();
      ConfigFileSettingsEditor.showDialog(configFileSettings, null, true, ContextVisibility.SHOW, KoronaHelp.CONFIG_FILE_SETTINGS);
      helpSystem.close();
   }
}
