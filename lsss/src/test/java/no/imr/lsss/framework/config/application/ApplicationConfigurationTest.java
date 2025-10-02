package no.imr.lsss.framework.config.application;

import no.imr.tools.upgrade.UpgradeException;
import no.imr.tools.upgrade.UpgradeTestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ApplicationConfigurationTest {
   @Test
   void fromVersion09() throws UpgradeException {
      doTestUpgrade("0.9", "1");
   }

   @Test
   void newestVersion() {
      // Update assertion and add test when the newest version is increased.
      assertEquals("1", ApplicationConfiguration.XML_UPGRADE_ENGINE.getTargetVersion());
   }

   private static void doTestUpgrade(String fromVersion, String toVersion) throws UpgradeException {
      UpgradeTestUtils.doTestUpgrade(ApplicationConfiguration.XML_UPGRADE_ENGINE, fromVersion, toVersion,
            "no/imr/lsss/framework/config/application/ApplicationConfigurationTest", "application", ".xml");
   }
}
