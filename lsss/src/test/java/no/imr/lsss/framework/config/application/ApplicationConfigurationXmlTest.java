package no.imr.lsss.framework.config.application;

import no.imr.lsss.LSSS;
import no.imr.lsss.test.LsssTestUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ApplicationConfigurationXmlTest {
   @Test
   void test() {
      LSSS lsss = LsssTestUtils.start();
      Element xml = lsss.getConfigurationManager().getApplicationConfiguration().toXml();
      ApplicationConfigurationXml applicationConfigurationXml = new ApplicationConfigurationXml(XmlUtils.toDocument(xml));
      assertNotNull(applicationConfigurationXml.lsssServerPortNode());
      assertNotNull(applicationConfigurationXml.lsssServerActiveNode());
      lsss.close();
   }
}
