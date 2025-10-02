package no.imr.korona.computation.plankton.editor;

import no.imr.korona.computation.plankton.PlanktonRectangle;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.time.DateTimeMillis;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

final class PlanktonRectangleTableModelTest {
   //Adds two PlanktonRectangle entries to PlanktonRectangleTableModel, change some values,
   //delete one row, check if structure changed
   //write to XML and compare to initial XML.

   @Test
   void testXml() throws IOException {
      String s1 = """
            <model name="test1" use="true" species="speciesTest">
               <start date="20060101" time="123459" depth="0"/>
               <stop date="20060101" time="131400" depth="100"/>
               <sizes> 12 34 ; 20 7.8e1 ; 40 0 ; 60  </sizes>
            </model>
            """;

      String s2 = """
            <model name="test2" use="false">
               <start date="20060102" time="223459" depth="1"/>
               <stop date="20060102" time="231400" depth="101.3"/>
               <sizes>22 44 ; 30 78 ; 40</sizes>
            </model>
            """;

      PlanktonRectangle planktonRectangle = new PlanktonRectangle(XmlUtils.readDocument(s1).getRootElement(), 1e-4);

      PlanktonRectangleTableModel tableModel = new PlanktonRectangleTableModel(1e-4);
      tableModel.setShowAll(true);
      tableModel.addRow(planktonRectangle, "testModel");

      assertEquals("testModel", tableModel.getRow(0).getAlgClass());
      PlanktonRectangle row0 = tableModel.getRow(0).getPlanktonRectangle();
      assertTrue(row0.isUse());
      assertEquals("speciesTest", row0.getSpecies());
      assertEquals(DateTimeMillis.toMillis(20060101, 123459 * 1000), (long) row0.getMillisRange().begin());
      assertEquals(DateTimeMillis.toMillis(20060101, 131400 * 1000), (long) row0.getMillisRange().end());
      assertEquals(new DefaultRange<>(0f, 100f), row0.getDepthRange());
      assertArrayEquals(new double[]{12e-4, 20e-4, 20e-4, 40e-4, 40e-4, 60e-4}, row0.getSizeHistogram().getDividers(), 1e-18);
      assertArrayEquals(new double[]{16e-4, 30e-4, 50e-4}, row0.getSizeHistogram().getCenters(), 1e-18);
      assertArrayEquals(new double[]{34, 78, 0}, row0.getSizeHistogram().getAbundances());

      PlanktonRectangle planktonRectangle2 = new PlanktonRectangle(XmlUtils.readDocument(s2).getRootElement(), 1e-4);
      tableModel.addRow(planktonRectangle2, "testModel2");

      assertEquals("testModel2", tableModel.getRow(1).getAlgClass());
      PlanktonRectangle row1 = tableModel.getRow(1).getPlanktonRectangle();
      assertFalse(row1.isUse());
      assertNull(row1.getSpecies());
      assertEquals(DateTimeMillis.toMillis(20060102, 223459 * 1000), (long) row1.getMillisRange().begin());
      assertEquals(DateTimeMillis.toMillis(20060102, 231400 * 1000), (long) row1.getMillisRange().end());
      assertEquals(new DefaultRange<>(1f, 101.3f), row1.getDepthRange());
      assertArrayEquals(new double[]{22e-4, 30e-4, 30e-4, 40e-4}, row1.getSizeHistogram().getDividers(), 1e-18);
      assertArrayEquals(new double[]{26e-4, 35e-4}, row1.getSizeHistogram().getCenters(), 1e-18);
      assertArrayEquals(new double[]{44, 78}, row1.getSizeHistogram().getAbundances());

      assertEquals(2, tableModel.getRowCount());
      assertEquals(18, tableModel.getColumnCount());

      row1.setSpecies("newSpecies");
      tableModel.deleteRow(0);

      assertEquals("testModel2", tableModel.getRow(0).getAlgClass());
      row0 = tableModel.getRow(0).getPlanktonRectangle();
      assertFalse(row0.isUse());
      assertEquals("newSpecies", row0.getSpecies());
      assertEquals(DateTimeMillis.toMillis(20060102, 223459 * 1000), (long) row0.getMillisRange().begin());
      assertEquals(DateTimeMillis.toMillis(20060102, 231400 * 1000), (long) row0.getMillisRange().end());
      assertEquals(new DefaultRange<>(1f, 101.3f), row0.getDepthRange());
      assertArrayEquals(new double[]{22e-4, 30e-4, 30e-4, 40e-4}, row0.getSizeHistogram().getDividers(), 1e-18);
      assertArrayEquals(new double[]{26e-4, 35e-4}, row0.getSizeHistogram().getCenters(), 1e-18);
      assertArrayEquals(new double[]{44, 78}, row0.getSizeHistogram().getAbundances());

      assertEquals(1, tableModel.getRowCount());
      assertEquals(16, tableModel.getColumnCount());

      row0.setSpecies(null);
      Element element = DocumentHelper.createElement("model")
            .addAttribute("name", "test2");
      row1.toXml(element, 1e-4);

      Document document = XmlUtils.readDocument(s2);

      assertTrue(XmlUtils.equalContent(document.getRootElement(), element));
   }
}
