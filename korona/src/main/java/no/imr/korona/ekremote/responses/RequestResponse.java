package no.imr.korona.ekremote.responses;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.ekremote.requests.ClientInfo;
import no.imr.korona.ekremote.requests.SequenceInfo;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;

import java.io.IOException;
import java.nio.ByteBuffer;

public record RequestResponse(
      String header,       //RES\0  [4]
      String request,      //REQ\0 [4]
      String msgControl,   //Sequence no, Current msg np, Total msg no [22]
      String msgResponse,  // XML based response text containing result of command request [1400]

      SequenceInfo sequenceInfo,
      Element responseXml,
      ClientInfo clientInfo,
      Fault fault
) implements Response {

   static final int BYTE_SIZE = 4 + 4 + 22 + 1400;

   static RequestResponse parse(String header, String request, ByteBuffer byteBuffer) throws IOException {
      String msgControl = ByteBufferUtils.readCString(byteBuffer, 22);
      String msgResponse = ByteBufferUtils.readCString(byteBuffer, 1400);

      String[] split = msgControl.split(",");
      SequenceInfo sequenceInfo = new SequenceInfo(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));

      Element responseXml = XmlUtils.readDocument(msgResponse).getRootElement();
      Element clientInfoElement = responseXml.element("clientInfo");
      int clientId = Integer.parseInt(clientInfoElement.elementText("cid"));
      int requestId = Integer.parseInt(clientInfoElement.elementText("rid"));
      ClientInfo clientInfo = new ClientInfo(clientId, requestId);
      Fault fault = Fault.parse(responseXml.element("fault"));
      return new RequestResponse(header, request, msgControl, msgResponse,
            sequenceInfo, responseXml, clientInfo, fault);
   }
}
