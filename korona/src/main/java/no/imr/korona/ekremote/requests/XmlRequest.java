package no.imr.korona.ekremote.requests;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;

public record XmlRequest(
      String header,      //REQ\0 [4]
      String msgControl,  //Sequence no, Current msg no, Total msg no [22]
      String msgRequest   //XML based command request [1400]
) {
   private static final int BYTE_SIZE = 4 + 22 + 1400;

   public XmlRequest(SequenceInfo sequenceInfo, ClientInfo clientInfo, MessageRequest<?> messageRequest) {
      this(
            "REQ",
            sequenceInfo.sequenceNumber() + "," + sequenceInfo.currentMessageNumber() + "," + sequenceInfo.totalMessageNumber(),
            messageRequest.getXmlString(clientInfo)
      );
   }

   public byte[] getBytes() {
      ByteBuffer byteBuffer = ByteBufferUtils.allocate(BYTE_SIZE);
      ByteBufferUtils.writeCString(byteBuffer, header, 4);
      ByteBufferUtils.writeCString(byteBuffer, msgControl, 22);
      ByteBufferUtils.writeCString(byteBuffer, msgRequest, 1400);
      return byteBuffer.array();
   }
}
