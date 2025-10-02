package no.imr.tools.xml;

import org.dom4j.DocumentException;

import java.io.IOException;

/**
 * Wrapper exception for {@link DocumentException}.
 */
public final class XmlException extends IOException {
   public XmlException(String message) {
      super(message);
   }

   XmlException(Exception e) {
      super(e);
   }
}
