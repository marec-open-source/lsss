package no.imr.lsss.modules;

import no.imr.lsss.LSSS;
import no.imr.lsss.test.LsssTestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ModuleManagerTest {
   @Test
   void test() {
      LSSS lsss = LsssTestUtils.start();
      ModuleManager moduleManager = lsss.getModuleManager();
      for (BaseLsssModule module : moduleManager.getModules()) {
         assertEquals(module.getName().persistentName(), module.getPersistentName());
         if (module instanceof BaseModuleOverlay overlay) {
            assertNotNull(overlay.getOverlaidModule());
         }
      }
   }
}
