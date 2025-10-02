package no.imr.lsss.framework;

import no.imr.tools.upgrade.UpgradeException;
import no.imr.tools.upgrade.UpgradeTestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SurveyManagerTest {
   @Test
   void fromVersion1() throws UpgradeException {
      doTestUpgrade("1.0", "2");
   }

   @Test
   void newestVersion() {
      // Update assertion and add test when the newest version is increased.
      assertEquals("2", SurveyManager.XML_SURVEY_FILE_UPGRADE_ENGINE.getTargetVersion());
   }

   private static void doTestUpgrade(String fromVersion, String toVersion) throws UpgradeException {
      UpgradeTestUtils.doTestUpgrade(SurveyManager.XML_SURVEY_FILE_UPGRADE_ENGINE, fromVersion, toVersion,
            "no/imr/lsss/framework/SurveyManagerTest", "SurveyFile", ".xml");
   }
}
