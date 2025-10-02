package no.imr.tools.parameter;

import no.imr.tools.logging.LogOnce;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Configurable {
   public static final String XML_NAME = "name";

   private final Name name;

   protected Configurable(Name name) {
      this.name = name;
   }

   public Name getName() {
      return name;
   }

   public Collection<? extends Configurable> getSubConfigurables() {
      return List.of();
   }

   public @Nullable Configurable getSubConfigurable(String persistentName) {
      return getSubConfigurables().stream()
            .filter(subConfigurable -> subConfigurable.getName().persistentName().equals(persistentName))
            .findFirst()
            .orElse(null);
   }

   /**
    * Tests whether {@link #toXml()} should be used.
    *
    * @return {@code true} if this instance should be persisted
    */
   public boolean isPersistable() {
      return true;
   }

   /**
    * Returns the XML representation of this instance.
    * This method should not return {@code null},
    * instead {@link #isPersistable()} should return {@code false} to indicate transience.
    *
    * @return an XML element, never {@code null}
    */
   public Element toXml() {
      Element element = createElement();
      for (Configurable subConfigurable : getSubConfigurables()) {
         if (subConfigurable.isPersistable()) {
            element.add(subConfigurable.toXml());
         }
      }
      return element;
   }

   public void fromXml(Element element) {
      Map<String, Configurable> map = createSubConfigurableMap();
      for (Element subElement : element.elements()) {
         String name = getName(subElement);

         Configurable subConfigurable = map.get(name);
         if (subConfigurable == null) {
            subConfigurable = possiblyCreateNewSubConfigurable(name);
            if (subConfigurable != null) {
               map.put(name, subConfigurable);
            }
         }

         if (subConfigurable != null) {
            subConfigurable.fromXml(subElement);
         } else {
            if (!handleUnknownXml(name, subElement)) {
               LogOnce.info("Ignoring XML for " + XmlUtils.getPath(subElement), this);
            }
         }
      }
   }

   public static String getName(Element element) {
      String nameAttribute = element.attributeValue(XML_NAME);
      return nameAttribute != null ? nameAttribute : element.getName();
   }

   protected @Nullable Configurable possiblyCreateNewSubConfigurable(String persistentName) {
      return null;
   }

   public boolean handleUnknownXml(String name, Element subElement) {
      return false;
   }

   private Map<String, Configurable> createSubConfigurableMap() {
      Map<String, Configurable> map = new HashMap<>();
      for (Configurable subConfigurable : getSubConfigurables()) {
         map.put(subConfigurable.getName().persistentName(), subConfigurable);
      }
      return map;
   }

   protected Element createElement() {
      return DocumentHelper.createElement(name.persistentName());
   }

   protected Element createElementWithNameAttribute(String elementName) {
      return DocumentHelper.createElement(elementName)
            .addAttribute(XML_NAME, name.persistentName());
   }
}
