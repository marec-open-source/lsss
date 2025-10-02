package no.imr.tools.xml;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class XmlParseTest {
   @Test
   void attribute() throws XmlParseException {
      Element element = DocumentHelper.createElement("e")
            .addAttribute("a", "aa")
            .addAttribute("b", "bb");
      assertEquals("aa", XmlParse.attribute(element, "a").getValue());
      assertEquals("bb", XmlParse.attribute(element, "b").getValue());
      assertEquals("aa", XmlParse.attribute(element, "a", "b").getValue());
      assertEquals("aa", XmlParse.attribute(element, "x", "a", "b").getValue());
      assertEquals("bb", XmlParse.attribute(element, "b", "a").getValue());
      assertThrows(XmlParseException.class, () -> XmlParse.attribute(element, "x").getValue());
      assertThrows(XmlParseException.class, () -> XmlParse.attribute(element, "x", "y").getValue());
   }
}
