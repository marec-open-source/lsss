package no.imr.korona.ekremote.requests.parameter;

import no.imr.korona.ekremote.responses.parameter.GetParameterResponse;
import org.dom4j.Element;

public record GetParameterRequest(
      String name
) implements ParameterServerRequest<GetParameterResponse> {

   public GetParameterRequest(ParameterServer.NormalParameter parameter) {
      this(parameter.getName());
   }

   @Override
   public String getXmlMethodString() {
      return "<GetParameter>" +
            "  <paramName>" + name + "</paramName>" +
            "  <time>0</time>" +
            "</GetParameter>";
   }

   @Override
   public String getResponseXmlElementName() {
      return "GetParameterResponse";
   }

   @Override
   public GetParameterResponse parseResponse(Element element) {
      return GetParameterResponse.parse(element);
   }
}
