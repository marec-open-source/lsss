package no.imr.tools.xml;

import java.io.IOException;

/**
 * Wrapper exception for XML errors.
 */
public final class XmlException extends IOException {
   XmlException(String message) {
      super(message);
   }

   XmlException(Exception e) {
      super(e);
   }
}
