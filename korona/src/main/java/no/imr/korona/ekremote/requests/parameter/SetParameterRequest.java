package no.imr.korona.ekremote.requests.parameter;

import no.imr.korona.ekremote.responses.parameter.SetParameterResponse;
import org.dom4j.Element;

public record SetParameterRequest(
      String name,
      String value,
      String type
) implements ParameterServerRequest<SetParameterResponse> {

   public SetParameterRequest(ParameterServer.NormalParameter parameter, String value) {
      this(parameter.getName(), value, parameter.getType());
   }

   public SetParameterRequest(ParameterServer.NormalParameter parameter, int value) {
      this(parameter, Integer.toString(value));
   }

   public SetParameterRequest(ParameterServer.PerChannelParameter parameter, String channelId, String value) {
      this(parameter.getName(channelId), value, parameter.getType());
   }

   @Override
   public String getXmlMethodString() {
      return "<SetParameter>" +
            "  <paramName>" + name + "</paramName>" +
            "  <paramValue>" + value + "</paramValue>" +
            "  <paramType>" + type + "</paramType>" +
            "</SetParameter>";
   }

   @Override
   public String getResponseXmlElementName() {
      return "SetParameterResponse";
   }

   @Override
   public SetParameterResponse parseResponse(Element element) {
      return new SetParameterResponse();
   }
}
