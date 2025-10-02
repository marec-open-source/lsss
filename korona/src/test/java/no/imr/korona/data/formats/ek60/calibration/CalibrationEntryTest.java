package no.imr.korona.data.formats.ek60.calibration;

import no.imr.tools.ResourceUtils;
import org.dom4j.Element;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class CalibrationEntryTest {
   @Test
   void testEquals() {
      Element element = ResourceUtils.getXml("no/imr/korona/data/formats/ek60/calibration/calibration.xml").getRootElement();
      CalibrationType c1 = CalibrationLoader.parseCalibrationContent(element).getDefaultType();
      CalibrationType c2 = CalibrationLoader.parseCalibrationContent(element).getDefaultType();

      long t0 = 0;
      long t1 = Instant.parse("2005-11-18T06:20:10Z").toEpochMilli();
      long t2 = Instant.parse("2005-11-18T09:00:03Z").toEpochMilli();

      assertNotEquals(c1.getEntry(t0), c1.getEntry(t1));
      assertNotEquals(c1.getEntry(t1), c1.getEntry(t2));

      testEquals(c1, c2, t0);
      testEquals(c1, c2, t1);
      testEquals(c1, c2, t2);
   }

   private static void testEquals(CalibrationType c1, CalibrationType c2, long t) {
      assertEquals(c1.getEntry(t), c2.getEntry(t));
      assertEquals(c1.getEntry(t).hashCode(), c2.getEntry(t).hashCode());
   }
}
