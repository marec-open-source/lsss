package no.imr.korona.computation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ModuleManagerTest {
   @Test
   void instantiateAll() throws ModuleCreationException {
      ModuleManager moduleManager = new ModuleManager();
      assertFalse(moduleManager.getModuleInfos().isEmpty());
      for (ModuleInfo moduleInfo : moduleManager.getModuleInfos()) {
         if (moduleInfo.isDeprecated()) {
            continue;
         }
         moduleManager.createModule(moduleInfo.getPersistentName());
      }
   }
}
