package no.imr.korona.ekremote.responses;

import com.google.common.base.Splitter;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;
import java.util.Map;

public record AliveResponse(
      String header,       //ALI\0  [4]
      String info,         // e.g. "ClientID:1,SeqNo:1\0" [1024]
      int sequenceNumber
) implements Response {

   static final int BYTE_SIZE = 4 + 1024;

   static AliveResponse parse(String header, ByteBuffer byteBuffer) {
      String info = ByteBufferUtils.readCString(byteBuffer, 1024);
      int sequenceNumber = 0;
      for (Map.Entry<String, String> entry : Splitter.on(',').omitEmptyStrings().withKeyValueSeparator(':').split(info).entrySet()) {
         String name = entry.getKey();
         String value = entry.getValue();
         switch (name) {
            case "SeqNo" -> {
               sequenceNumber = Integer.parseInt(value);
            }
            default -> {
               // Ignoring the rest
            }
         }
      }
      return new AliveResponse(header, info, sequenceNumber);
   }
}
