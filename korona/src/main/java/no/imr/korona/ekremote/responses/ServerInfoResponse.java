package no.imr.korona.ekremote.responses;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;

public record ServerInfoResponse(
      String header,                   // "SI2\0"
      String applicationType,          // [64]
      String applicationName,          // Name of the current application [64]
      String applicationDescription,   // Description of the current application [128]
      int applicationID,               // ID of the current application
      int commandPort,                 // Port number to send commands to
      int mode,                        // If the application is running against the local data source or a remote data source
      String hostName                  // IP address of the computer the application is running on [64]
) implements Response {

   static final int BYTE_SIZE = 4 + 64 + 64 + 128 + 4 + 4 + 4 + 64;

   static ServerInfoResponse parse(String header, ByteBuffer byteBuffer) {
      return new ServerInfoResponse(
            header,
            ByteBufferUtils.readCString(byteBuffer, 64),
            ByteBufferUtils.readCString(byteBuffer, 64),
            ByteBufferUtils.readCString(byteBuffer, 128),
            byteBuffer.getInt(),
            byteBuffer.getInt(),
            byteBuffer.getInt(),
            ByteBufferUtils.readCString(byteBuffer, 64)
      );
   }
}
