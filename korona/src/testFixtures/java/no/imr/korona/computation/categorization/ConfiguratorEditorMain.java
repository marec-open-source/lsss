package no.imr.korona.computation.categorization;

import no.imr.korona.Korona;
import no.imr.korona.resources.KoronaResource;
import no.imr.korona.test.KoronaTestUtils;
import no.imr.korona.viewer.KoronaHelpSystem;
import no.imr.tools.Utils;
import no.imr.tools.help.HelpSystem;

import javax.swing.SwingUtilities;

final class ConfiguratorEditorMain {
   private ConfiguratorEditorMain() {
   }

   static void main(String[] args) {
      Utils.init(args, KoronaResource.KORONA_64);
      SwingUtilities.invokeLater(ConfiguratorEditorMain::start);
   }

   private static void start() {
      Korona korona = new Korona();
      HelpSystem helpSystem = KoronaHelpSystem.createHelpSystem(korona);
      ConfiguratorEditor.editCategorizationConfiguration(null, KoronaTestUtils.createConfigurator(korona));
      helpSystem.close();
   }
}
