package no.imr.tools.xml;

import no.imr.tools.test.JUnitUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.DocumentSource;
import org.junit.jupiter.api.Test;

import javax.xml.transform.TransformerException;
import java.io.IOException;

final class XslTransformerTest {
   @Test
   void test() throws IOException, TransformerException {
      String xsl = """
            <xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
               <xsl:template match='@*|node()'>
                  <xsl:copy>
                     <xsl:apply-templates select='@*|node()'/>
                  </xsl:copy>
               </xsl:template>

               <xsl:template match='a'>
                  <xsl:element name='x'>
                     <xsl:apply-templates select='@*|node()'/>
                  </xsl:element>
               </xsl:template>

               <xsl:template match='@c'>
                  <xsl:attribute name='x'>
                     <xsl:value-of select='.'/>
                  </xsl:attribute>
               </xsl:template>
            </xsl:stylesheet>
            """;

      String inputXml = """
            <a ab='ac'>
               <b c='d' e='f'/>
            </a>
            """;

      String expectedOutputXml = """
            <x ab='ac'>
               <b x='d' e='f'/>
            </x>
            """;

      doTest(xsl, inputXml, expectedOutputXml);
   }

   @Test
   void testNotRootElement() throws IOException, TransformerException {
      String xsl = """
            <xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
               <xsl:template match='@*|node()'>
                  <xsl:copy>
                     <xsl:apply-templates select='@*|node()'/>
                  </xsl:copy>
               </xsl:template>

               <xsl:template match='a'>
                  <xsl:element name='x'>
                     <xsl:apply-templates select='@*|node()'/>
                  </xsl:element>
               </xsl:template>
            </xsl:stylesheet>
            """;

      String inputXml = """
            <a b='c'>
               <a d='e'/>
            </a>
            """;
      Element inputElement = XmlUtils.readDocument(inputXml).getRootElement().element("a");

      String expectedOutputXml = """
            <x d='e'/>
            """;

      doTest(inputElement, xsl, expectedOutputXml);
   }

   @Test
   void testNoDocument() throws IOException, TransformerException {
      String xsl = """
            <xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
               <xsl:template match='@*|node()'>
                  <xsl:copy>
                     <xsl:apply-templates select='@*|node()'/>
                  </xsl:copy>
               </xsl:template>

               <xsl:template match='a'>
                  <xsl:element name='x'>
                     <xsl:apply-templates select='@*|node()'/>
                  </xsl:element>
               </xsl:template>
            </xsl:stylesheet>
            """;

      Element inputElement = DocumentHelper.createElement("a");

      String expectedOutputXml = """
            <x/>
            """;

      doTest(inputElement, xsl, expectedOutputXml);
   }

   private static void doTest(String xsl, String inputXml, String expectedOutputXml) throws IOException, TransformerException {
      doTest(XmlUtils.readDocument(inputXml).getRootElement(), xsl, expectedOutputXml);
   }

   private static void doTest(Element inputElement, String xsl, String expectedOutputXml) throws IOException, TransformerException {
      XslTransformer transformer = new XslTransformer(new DocumentSource(XmlUtils.readDocument(xsl)));
      Element out = transformer.transform(inputElement);
      Element expected = XmlUtils.readDocument(expectedOutputXml).getRootElement();
      JUnitUtils.assertEquals(expected, out);
   }
}
