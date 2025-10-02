package no.imr.lsss.framework.config.survey;

import no.imr.tools.upgrade.UpgradeException;
import no.imr.tools.upgrade.UpgradeTestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SurveyConfigurationTest {
   @Test
   void fromVersion09() throws UpgradeException {
      doTestUpgrade("0.9", "1");
   }

   @Test
   void fromVersion1() throws UpgradeException {
      doTestUpgrade("1", "2");
   }

   @Test
   void fromVersion2() throws UpgradeException {
      doTestUpgrade("2", "3");
   }

   @Test
   void fromVersion3() throws UpgradeException {
      doTestUpgrade("3", "4");
   }

   @Test
   void newestVersion() {
      // Update assertion and add test when the newest version is increased.
      assertEquals("4", SurveyConfiguration.XML_UPGRADE_ENGINE.getTargetVersion());
   }

   private static void doTestUpgrade(String fromVersion, String toVersion) throws UpgradeException {
      UpgradeTestUtils.doTestUpgrade(SurveyConfiguration.XML_UPGRADE_ENGINE, fromVersion, toVersion,
            "no/imr/lsss/framework/config/survey/SurveyConfigurationTest", "survey", ".xml");
   }
}
