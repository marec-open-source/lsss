package no.imr.tools.xml;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Node;

/**
 * Utility functions for parsing XML.
 */
public final class XmlParse {
   private XmlParse() {
   }

   public static Element element(Element element, String name) throws XmlParseException {
      Element child = element.element(name);
      if (child == null) {
         throw new XmlParseException(element, name);
      }
      return child;
   }

   public static Attribute attribute(Element element, String name) throws XmlParseException {
      Attribute attribute = element.attribute(name);
      if (attribute == null) {
         throw new XmlParseException(element, name);
      }
      return attribute;
   }

   public static Attribute attribute(Element element, String... alternativeNames) throws XmlParseException {
      for (String name : alternativeNames) {
         Attribute attribute = element.attribute(name);
         if (attribute != null) {
            return attribute;
         }
      }
      throw new XmlParseException(element, alternativeNames[0]);
   }

   //-------- String

   public static String stringAttribute(Element element, String name) throws XmlParseException {
      return attribute(element, name).getValue();
   }

   public static String stringAttribute(Element element, String name, String defaultValue) {
      Attribute attribute = element.attribute(name);
      return attribute != null ? attribute.getValue() : defaultValue;
   }

   public static String stringElement(Element element, String name) throws XmlParseException {
      return element(element, name).getText();
   }

   public static String stringElement(Element element, String name, String defaultValue) {
      Element child = element.element(name);
      return child != null ? child.getText() : defaultValue;
   }

   //-------- Boolean

   public static boolean booleanAttribute(Element element, String name, boolean defaultValue) {
      Attribute attribute = element.attribute(name);
      return attribute != null ? Boolean.parseBoolean(attribute.getValue()) : defaultValue;
   }

   public static boolean booleanElement(Element element, String name, boolean defaultValue) {
      Element child = element.element(name);
      return child != null ? Boolean.parseBoolean(child.getText()) : defaultValue;
   }

   //-------- Byte

   public static byte byteElement(Element element, String name, byte defaultValue) throws XmlParseException {
      Element child = element.element(name);
      return child != null ? parseByte(child) : defaultValue;
   }

   //-------- Short

   public static short shortAttribute(Element element, String name) throws XmlParseException {
      return parseShort(attribute(element, name));
   }

   public static short shortAttribute(Element element, String name, short defaultValue) throws XmlParseException {
      Attribute attribute = element.attribute(name);
      return attribute != null ? parseShort(attribute) : defaultValue;
   }

   //-------- Integer

   public static int intAttribute(Element element, String name) throws XmlParseException {
      return parseInt(attribute(element, name));
   }

   public static int intAttribute(Element element, String name, int defaultValue) throws XmlParseException {
      Attribute attribute = element.attribute(name);
      return attribute != null ? parseInt(attribute) : defaultValue;
   }

   //-------- Float

   public static float floatAttribute(Element element, String name) throws XmlParseException {
      return parseFloat(attribute(element, name));
   }

   public static float floatAttribute(Element element, String name, float defaultValue) throws XmlParseException {
      Attribute attribute = element.attribute(name);
      return attribute != null ? parseFloat(attribute) : defaultValue;
   }

   public static float floatElement(Element element, String name, float defaultValue) throws XmlParseException {
      Element child = element.element(name);
      return child != null ? parseFloat(child) : defaultValue;
   }

   //-------- Double

   public static double doubleAttribute(Element element, String name) throws XmlParseException {
      return parseDouble(attribute(element, name));
   }

   public static double doubleElement(Element element, String name) throws XmlParseException {
      return parseDouble(element(element, name));
   }

   //-------- Parsing numbers

   public static byte parseByte(Node node) throws XmlParseException {
      try {
         return Byte.parseByte(node.getText());
      } catch (NumberFormatException e) {
         throw new XmlParseException(node.getParent(), node.getName(), e);
      }
   }

   public static short parseShort(Node node) throws XmlParseException {
      try {
         return Short.parseShort(node.getText());
      } catch (NumberFormatException e) {
         throw new XmlParseException(node.getParent(), node.getName(), e);
      }
   }

   public static int parseInt(Node node) throws XmlParseException {
      try {
         return Integer.parseInt(node.getText());
      } catch (NumberFormatException e) {
         throw new XmlParseException(node.getParent(), node.getName(), e);
      }
   }

   public static float parseFloat(Node node) throws XmlParseException {
      try {
         return Float.parseFloat(node.getText());
      } catch (NumberFormatException e) {
         throw new XmlParseException(node.getParent(), node.getName(), e);
      }
   }

   public static double parseDouble(Node node) throws XmlParseException {
      try {
         return Double.parseDouble(node.getText());
      } catch (NumberFormatException e) {
         throw new XmlParseException(node.getParent(), node.getName(), e);
      }
   }
}
