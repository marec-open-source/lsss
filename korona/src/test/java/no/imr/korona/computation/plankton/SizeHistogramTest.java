package no.imr.korona.computation.plankton;

import no.imr.tools.range.DefaultRange;
import no.imr.tools.xml.XmlUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class SizeHistogramTest {
   @Test
   void testXml() throws IOException {
      String s = """
            <model name='test' use='true'>
               <start date='20060101' time='123459' depth='0'/>
               <stop date='20060101' time='131400' depth='100'/>
               <sizes> 12 34  ; 20 ;  \t   7.8e1 40 0 60  </sizes>
            </model>
            """;
      PlanktonRectangle planktonRectangle = new PlanktonRectangle(XmlUtils.readDocument(s).getRootElement(), 1e-4);
      assertTrue(planktonRectangle.isUse());
      assertEquals(Instant.parse("2006-01-01T12:34:59Z"), planktonRectangle.getTimeRange().begin());
      assertEquals(Instant.parse("2006-01-01T13:14:00Z"), planktonRectangle.getTimeRange().end());
      assertEquals(new DefaultRange<>(0f, 100f), planktonRectangle.getDepthRange());
      assertArrayEquals(new double[]{12e-4, 20e-4, 20e-4, 40e-4, 40e-4, 60e-4}, planktonRectangle.getSizeHistogram().getDividers(), 1e-18);
      assertArrayEquals(new double[]{16e-4, 30e-4, 50e-4}, planktonRectangle.getSizeHistogram().getCenters(), 1e-18);
      assertArrayEquals(new double[]{34, 78, 0}, planktonRectangle.getSizeHistogram().getAbundances());
   }
}
