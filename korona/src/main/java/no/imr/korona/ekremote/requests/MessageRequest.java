package no.imr.korona.ekremote.requests;

import no.imr.korona.ekremote.responses.MessageResponse;
import org.dom4j.Element;

public interface MessageRequest<T extends MessageResponse> {

   String getTargetComponent();

   String getXmlMethodString();

   String getResponseXmlElementName();

   T parseResponse(Element element);

   default String getXmlString(ClientInfo clientInfo) {
      return "<request>"
            + "  <clientInfo>"
            + "    <cid>" + clientInfo.clientId() + "</cid>"
            + "    <rid>" + clientInfo.requestId() + "</rid>"
            + "  </clientInfo>"
            + "  <type>invokeMethod</type>"
            + "  <targetComponent>" + getTargetComponent() + "</targetComponent>"
            + "  <method>" + getXmlMethodString() + "</method>"
            + "</request>";
   }
}
