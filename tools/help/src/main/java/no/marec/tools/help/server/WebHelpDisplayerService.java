package no.marec.tools.help.server;

import no.imr.tools.help.HelpDisplayer;
import no.imr.tools.help.HelpDisplayerService;
import no.imr.tools.help.HelpSystem;
import no.imr.tools.help.HelpSystemHelpSet;
import no.imr.tools.logging.Log;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public final class WebHelpDisplayerService implements HelpDisplayerService {
   public WebHelpDisplayerService() {
   }

   @Override
   public HelpDisplayer createHelpSystemDisplayer(HelpSystem helpSystem) {
      try {
         List<String> helpDirs = helpSystem.getHelpSets().stream()
               .map(HelpSystemHelpSet::getHelpDir)
               .filter(Objects::nonNull)
               .toList();
         List<ClassLoader> classLoaders = helpSystem.getHelpSets().stream()
               .map(HelpSystemHelpSet::getClassLoader)
               .filter(Objects::nonNull)
               .toList();
         return new WebHelpDisplayer(helpDirs, classLoaders, helpSystem.getHelpSystemInfo());
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error creating web help server", e);
         return HelpDisplayer.noneAvailable();
      }
   }
}
