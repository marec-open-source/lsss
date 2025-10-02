package no.imr.lsss.database.reports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ReportUtilsTest {
   @Test
   void latitudeString() {
      assertEquals("N00:00.0", ReportUtils.latitudeString(0));
      assertEquals("S01:14.1", ReportUtils.latitudeString(-1.23456f));
      assertEquals("N89:59.9", ReportUtils.latitudeString(89.999f));
   }

   @Test
   void longitudeString() {
      assertEquals("E000:00.0", ReportUtils.longitudeString(0));
      assertEquals("W001:14.1", ReportUtils.longitudeString(-1.23456f));
      assertEquals("E179:59.9", ReportUtils.longitudeString(179.999f));
   }
}
