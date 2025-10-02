package no.imr.tools.upgrade;

import no.imr.tools.ResourceUtils;
import no.imr.tools.test.JUnitUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;

import static org.junit.jupiter.api.Assertions.*;

public final class UpgradeTestUtils {
   private UpgradeTestUtils() {
   }

   public static void doTestUpgrade(UpgradeEngine<Element> upgradeEngine, String fromVersion, String toVersion,
                                    String testDirectory, String filePrefix, String fileSuffix) throws UpgradeException {
      String prefix = testDirectory + "/" + fromVersion + "-" + toVersion + "/" + filePrefix;
      Element input = ResourceUtils.getXml(prefix + "-in" + fileSuffix).getRootElement();
      Element expected = ResourceUtils.getXml(prefix + "-out" + fileSuffix).getRootElement();
      assertEquals(fromVersion, XmlUtils.getVersion(input));
      assertEquals(toVersion, XmlUtils.getVersion(expected));
      Element upgradedOnce = upgradeEngine.createUpgrader(fromVersion).upgrade(input);
      JUnitUtils.assertEquals(expected, upgradedOnce);

      Element upgradedFully = upgradeEngine.upgrade(input);
      assertEquals(upgradeEngine.getTargetVersion(), XmlUtils.getVersion(upgradedFully));
   }
}
