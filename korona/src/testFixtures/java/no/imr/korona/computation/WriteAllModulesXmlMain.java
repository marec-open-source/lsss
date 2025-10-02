package no.imr.korona.computation;

import no.imr.korona.Korona;
import no.imr.tools.test.JUnitUtils;

import java.io.IOException;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Writes the default configuration of all modules.
 */
final class WriteAllModulesXmlMain {
   private WriteAllModulesXmlMain() {
   }

   public static void main(String[] args) throws ModuleCreationException, IOException {
      Korona korona = new Korona();

      NavigableSet<String> persistentNames = korona.getModuleManager().getModuleInfos().stream()
            .map(ModuleInfo::getPersistentName)
            .collect(Collectors.toCollection(TreeSet::new));

      ModuleContainer moduleContainer = new ModuleContainer(korona);
      for (String persistentName : persistentNames) {
         BaseModule module = korona.getModuleManager().createModule(persistentName);
         moduleContainer.addModule(module);
      }
      moduleContainer.writeConfiguration(JUnitUtils.OUTPUT_DIR.resolve("AllModules.cds"));
   }
}
