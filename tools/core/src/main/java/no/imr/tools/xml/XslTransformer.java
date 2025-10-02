package no.imr.tools.xml;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

/**
 * Transforms XML.
 */
public final class XslTransformer {
   private final Transformer transformer;

   public XslTransformer(Source source) throws TransformerConfigurationException {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      transformer = transformerFactory.newTransformer(source);
   }

   public Element transform(Element element) throws TransformerException {
      Document document = element.getDocument();
      if (document == null || document.getRootElement() != element) {
         document = XmlUtils.toDocument((Element) element.clone());
      }
      //Note: new DocumentSource(element) => new DocumentSource(element.getDocument())

      DocumentSource source = new DocumentSource(document);

      DocumentResult result = new DocumentResult();
      transformer.transform(source, result);

      Document transformedDoc = result.getDocument();
      return transformedDoc.getRootElement();
   }
}
