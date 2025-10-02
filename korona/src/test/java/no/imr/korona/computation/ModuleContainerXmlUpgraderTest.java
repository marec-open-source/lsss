package no.imr.korona.computation;

import no.imr.tools.upgrade.UpgradeException;
import no.imr.tools.upgrade.UpgradeTestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ModuleContainerXmlUpgraderTest {
   @Test
   void fromVersion0() throws UpgradeException {
      doTestUpgrade("0", "1", "DepthModule");
      doTestUpgrade("0", "1", "AllModules");
   }

   @Test
   void fromVersion1() throws UpgradeException {
      doTestUpgrade("1", "2", "TrackingModule");
   }

   @Test
   void fromVersion2() throws UpgradeException {
      doTestUpgrade("2", "3", "CombinationRemoverModule");
      doTestUpgrade("2", "3", "RemoveSingleChannelModule");
   }

   @Test
   void fromVersion3() throws UpgradeException {
      doTestUpgrade("3", "4", "SchoolDetectionModule");
   }

   @Test
   void newestVersion() {
      // Update assertion and add test when the newest version is increased.
      assertEquals("4", ModuleContainer.XML_UPGRADE_ENGINE.getTargetVersion());
   }

   private static void doTestUpgrade(String fromVersion, String toVersion, String testNamePrefix) throws UpgradeException {
      UpgradeTestUtils.doTestUpgrade(ModuleContainer.XML_UPGRADE_ENGINE, fromVersion, toVersion,
            "no/imr/korona/computation/ModuleContainerXmlUpgraderTest", testNamePrefix, ".cds");
   }
}
