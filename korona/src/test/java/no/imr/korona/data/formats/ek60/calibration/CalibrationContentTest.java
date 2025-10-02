package no.imr.korona.data.formats.ek60.calibration;

import no.imr.tools.ResourceUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

final class CalibrationContentTest {
   @Test
   void xml() throws IOException {
      String xmlString = ResourceUtils.getString("no/imr/korona/data/formats/ek60/calibration/calibration.xml");
      Element element = XmlUtils.readDocument(xmlString).getRootElement();
      CalibrationContent calibrationContent = CalibrationLoader.parseCalibrationContent(element);
      assertEquals(xmlString, XmlUtils.toPrettyString(XmlUtils.toDocument(calibrationContent.toXml())));
   }
}
