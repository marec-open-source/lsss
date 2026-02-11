package no.imr.korona.data.formats.ek500;

import no.imr.korona.resources.KoronaResource;
import no.imr.tools.Utils;

import javax.swing.SwingUtilities;

final class EK500SettingGUIMain {
   private EK500SettingGUIMain() {
   }

   static void main(String[] args) {
      Utils.init(args, KoronaResource.KORONA_64);
      SwingUtilities.invokeLater(EK500SettingGUIMain::start);
   }

   private static void start() {
      new EK500SettingsGUI(EK500Settings.getDefaultEK500SettingsFile())
            .show(null);
   }
}
