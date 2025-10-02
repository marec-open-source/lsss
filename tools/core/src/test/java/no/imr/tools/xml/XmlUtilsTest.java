package no.imr.tools.xml;

import no.imr.tools.Utils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

final class XmlUtilsTest {
   /**
    * This test failed before using UTF-8 in {@link XmlUtils#readDocument(String)}.
    */
   @Test
   void string() throws IOException {
      Element element = DocumentHelper.createElement("a")
            .addText("æøå");

      String s = XmlUtils.toCompactString(element);
      Document document = XmlUtils.readDocument(s);

      assertEquals(s, XmlUtils.toCompactString(document.getRootElement()));
      assertTrue(XmlUtils.equalContent(element, document.getRootElement()), s);
      assertTrue(XmlUtils.equalContent(XmlUtils.toDocument(element), document), s);
   }

   @Test
   void ignoreDTD() throws IOException {
      String s = """
            <!DOCTYPE root PUBLIC "-//non-existing//EN" "https://internal.marec.no/non-existing.dtd">
            <root>a</root>
            """;
      XmlUtils.readDocument(s);
      XmlUtils.readDocument(s.getBytes(Utils.UTF_8));
   }

   @Test
   void removeBlankMixedContentText() throws IOException {
      Document document = XmlUtils.readDocument("<a> \t <b> </b> \n </a>");
      Element root = document.getRootElement();
      XmlUtils.removeBlankMixedContentText(root);
      assertEquals(1, root.nodeCount());
      assertEquals(" ", root.element("b").getText());
   }

   @Test
   void xpath() throws IOException {
      // This test fails if Jaxen is missing.
      Document document = XmlUtils.readDocument("<a><b>c</b></a>");
      assertEquals("c", document.selectSingleNode("//b/text()").getText());
   }
}
