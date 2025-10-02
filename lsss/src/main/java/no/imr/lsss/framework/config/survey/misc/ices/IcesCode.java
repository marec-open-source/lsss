package no.imr.lsss.framework.config.survey.misc.ices;

import org.dom4j.Element;

public record IcesCode(String key, String description) {

   IcesCode(Element element) {
      this(element.elementText("Key"), element.elementText("Description"));
   }
}
