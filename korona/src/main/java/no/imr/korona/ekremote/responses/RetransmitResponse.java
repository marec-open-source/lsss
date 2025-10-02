package no.imr.korona.ekremote.responses;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;

public record RetransmitResponse(
      String header,       //RTR\0  [4]
      String info          // e.g. "ClientID:1,SeqNo:1\0" [1024]
) implements Response {

   static final int BYTE_SIZE = 4 + 1024;

   static RetransmitResponse parse(String header, ByteBuffer byteBuffer) {
      String info = ByteBufferUtils.readCString(byteBuffer, 1024);
      return new RetransmitResponse(header, info);
   }
}
