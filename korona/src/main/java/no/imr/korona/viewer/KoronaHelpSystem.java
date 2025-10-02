package no.imr.korona.viewer;

import no.imr.korona.Korona;
import no.imr.korona.plugins.ModulePlugin;
import no.imr.tools.help.HelpSystem;
import no.imr.tools.help.HelpSystemInfo;
import no.imr.tools.help.HelpSystemPort;

public final class KoronaHelpSystem {
   private KoronaHelpSystem() {
   }

   public static HelpSystem createHelpSystem(Korona korona) {
      HelpSystem helpSystem = new HelpSystem(new HelpSystemInfo(HelpSystemPort.KORONA, "korona", Korona.VERSION));
      for (ModulePlugin plugin : korona.getModuleManager().getModulePlugins()) {
         helpSystem.addHelpSet(plugin.getHelpSet());
      }
      return helpSystem;
   }
}
