package no.imr.korona.ekremote.responses;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.tools.Max;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ResponseFactory {
   private static final String ALI = "ALI"; // Alive response header
   private static final String CON = "CON"; // Request field of connect response
   private static final String RES = "RES"; // Normal response header
   private static final String REQ = "REQ"; // Request field of a request response
   private static final String RTR = "RTR"; // Retransmit header
   private static final String SI2 = "SI2"; // Server info

   public static final int MAX_BYTE_SIZE = Max.of(
         AliveResponse.BYTE_SIZE,
         ConnectResponse.BYTE_SIZE,
         RequestResponse.BYTE_SIZE,
         RetransmitResponse.BYTE_SIZE,
         ServerInfoResponse.BYTE_SIZE
   );

   private ResponseFactory() {
   }

   public static Response parse(byte[] bytes) throws IOException {
      try {
         return tryParse(bytes);
      } catch (BufferUnderflowException e) {
         throw new IOException(e);
      }
   }

   private static Response tryParse(byte[] bytes) throws IOException {
      ByteBuffer byteBuffer = ByteBuffer.wrap(bytes)
            .order(ByteOrder.LITTLE_ENDIAN);
      String header = ByteBufferUtils.readCString(byteBuffer, 4);
      return switch (header) {
         case ALI -> AliveResponse.parse(header, byteBuffer);

         case RES -> {
            String request = ByteBufferUtils.readCString(byteBuffer, 4);
            yield switch (request) {
               case CON -> ConnectResponse.parse(header, request, byteBuffer);
               case REQ -> RequestResponse.parse(header, request, byteBuffer);
               default -> UnknownResponse.parse(header, request, byteBuffer);
            };
         }

         case RTR -> RetransmitResponse.parse(header, byteBuffer);

         case SI2 -> ServerInfoResponse.parse(header, byteBuffer);

         default -> UnknownResponse.parse(header, null, byteBuffer);
      };
   }
}
