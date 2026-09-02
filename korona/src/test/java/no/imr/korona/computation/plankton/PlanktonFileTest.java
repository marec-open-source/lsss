package no.imr.korona.computation.plankton;

import no.imr.tools.range.RangeMap;
import no.imr.tools.xml.XmlUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

final class PlanktonFileTest {
   @Test
   void testA() throws IOException {
      String s = """
            <plankton>
               <model name='test' use='true'>
                  <start date='20060101' time='123459' depth='1'/>
                  <stop date='20060101' time='131400' depth='100'/>
                  <sizes>1 88 2 99 3</sizes>
               </model>
            </plankton>
            """;
      PlanktonFile planktonFile = new PlanktonFile(XmlUtils.readDocument(s));

      assertEquals(0, planktonFile.getDepthMap("test", Instant.parse("2006-01-01T12:34:58Z")).size());
      assertEquals(0, planktonFile.getDepthMap("test", Instant.parse("2006-01-01T13:14:00Z")).size());

      RangeMap<Float, PlanktonRectangle> planktonRectangleMap = planktonFile.getDepthMap("test", Instant.parse("2006-01-01T12:59:59Z"));
      assertEquals(1, planktonRectangleMap.size());
      assertNull(planktonRectangleMap.get(0.99f));
      PlanktonRectangle planktonRectangle = planktonRectangleMap.get(1f);
      assertNotNull(planktonRectangle);
      assertArrayEquals(new double[]{1, 2, 2, 3}, planktonRectangle.getSizeHistogram().getDividers());
      assertArrayEquals(new double[]{88, 99}, planktonRectangle.getSizeHistogram().getAbundances());
      assertNull(planktonRectangleMap.get(100f));
   }

   @Test
   void testB() throws IOException {
      String s = """
            <plankton sizeFactor='10'>
               <model name='test' use='true'>
                  <sizes>1 0 9</sizes>
               </model>
               <model name='test' use='false'>
                  <sizes>2 0 9</sizes>
               </model>
               <model name='test' use='true'>
                  <start date='20060101' time='123459' depth='1'/>
                  <stop date='20060101' time='131400' depth='50'/>
                  <sizes>3 0 9</sizes>
               </model>
               <model name='test' use='true'>
                  <start date='20060101' time='123459' depth='10'/>
                  <stop date='20060101' time='131400' depth='100'/>
                  <sizes>4 0 9</sizes>
               </model>
            </plankton>
            """;
      PlanktonFile planktonFile = new PlanktonFile(XmlUtils.readDocument(s));

      assertArrayEquals(new double[]{10, 90}, Objects.requireNonNull(planktonFile.getDepthMap("test", Instant.parse("2006-01-01T12:34:58Z")).get(1f)).getSizeHistogram().getDividers());
      assertArrayEquals(new double[]{30, 90}, Objects.requireNonNull(planktonFile.getDepthMap("test", Instant.parse("2006-01-01T12:34:59Z")).get(1f)).getSizeHistogram().getDividers());
      assertArrayEquals(new double[]{10, 90}, Objects.requireNonNull(planktonFile.getDepthMap("test", Instant.parse("2006-01-01T13:14:00Z")).get(1f)).getSizeHistogram().getDividers());

      assertArrayEquals(new double[]{10, 90}, Objects.requireNonNull(planktonFile.getDepthMap("test", Instant.parse("2006-01-01T12:34:59Z")).get(0f)).getSizeHistogram().getDividers());
      assertArrayEquals(new double[]{30, 90}, Objects.requireNonNull(planktonFile.getDepthMap("test", Instant.parse("2006-01-01T12:34:59Z")).get(1f)).getSizeHistogram().getDividers());
      assertArrayEquals(new double[]{40, 90}, Objects.requireNonNull(planktonFile.getDepthMap("test", Instant.parse("2006-01-01T12:34:59Z")).get(10f)).getSizeHistogram().getDividers());
      assertArrayEquals(new double[]{10, 90}, Objects.requireNonNull(planktonFile.getDepthMap("test", Instant.parse("2006-01-01T12:34:59Z")).get(100f)).getSizeHistogram().getDividers());
   }
}
