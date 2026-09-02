package no.imr.tools.xml;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

public final class XmlParseException extends Exception {
   public XmlParseException(Element element, String name) {
      this(element, name, null);
   }

   public XmlParseException(@Nullable Element element, String name, @Nullable Throwable cause) {
      super("Error with " + name + (element != null ? " in " + XmlUtils.getPath(element) : ""), cause);
   }

   public XmlParseException(Attribute attribute) {
      this(attribute, null);
   }

   public XmlParseException(Attribute attribute, @Nullable Throwable cause) {
      this(attribute.getParent(), attribute.getName(), cause);
   }
}
