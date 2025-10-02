package no.imr.korona.ekremote.responses.parameter;

import org.dom4j.Element;

public record GetParameterResponse(
      String value,
      String type,
      String time
) implements ParameterServerResponse {

   public static GetParameterResponse parse(Element element) {
      Element paramValueElement = element.element("paramValue");
      Element valueElement = paramValueElement.element("value");
      String value = valueElement != null ? valueElement.getText() : "";
      String type = valueElement != null ? valueElement.attributeValue("dt") : "";
      String time = paramValueElement.elementText("time");
      return new GetParameterResponse(value, type, time);
   }
}
