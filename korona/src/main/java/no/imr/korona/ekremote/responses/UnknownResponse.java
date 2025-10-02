package no.imr.korona.ekremote.responses;

import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

public record UnknownResponse(
      String header,
      @Nullable String request,
      byte[] content
) implements Response {

   static UnknownResponse parse(String header, @Nullable String request, ByteBuffer byteBuffer) {
      byte[] content = new byte[byteBuffer.remaining()];
      byteBuffer.get(content);
      return new UnknownResponse(header, request, content);
   }

   @Override
   public String toString() {
      return header + ", " + (request != null ? request + ", " : "") + content.length + " bytes";
   }
}
