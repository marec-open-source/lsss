package no.imr.korona.ekremote.responses;

import com.google.common.base.Splitter;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.tools.logging.Log;

import java.nio.ByteBuffer;
import java.util.Map;

public record ConnectResponse(
      String header,       //RES\0  [4]
      String request,      //CON\0  [4]
      String msgControl,   //      [22]
      String msgResponse,  // Response text containing result of the connection request [1400]
      String resultCode,
      int clientId,
      int accessLevel
) implements Response {

   static final int BYTE_SIZE = 4 + 4 + 22 + 1400;

   private static final String S_OK = "S_OK";
   private static final String E_ACCESSDENIED = "E_ACCESSDENIED";
   private static final String E_FAIL = "E_FAIL";

   static ConnectResponse parse(String header, String request, ByteBuffer byteBuffer) {
      String msgControl = ByteBufferUtils.readCString(byteBuffer, 22);
      String msgResponse = ByteBufferUtils.readCString(byteBuffer, 1400);

      int firstCommaIndex = msgResponse.indexOf(',');
      String resultCodeStr = msgResponse.substring(0, firstCommaIndex);

      String[] resultCodePair = resultCodeStr.split(":");
      String resultCode = resultCodePair[1];
      int clientId = 0;
      int accessLevel = 0;
      if (S_OK.equals(resultCode)) {
         int begin = msgResponse.indexOf('{');
         int end = msgResponse.lastIndexOf('}');
         String parameters = msgResponse.substring(begin + 1, end);
         for (Map.Entry<String, String> entry : Splitter.on(',').omitEmptyStrings().withKeyValueSeparator(':').split(parameters).entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            switch (name) {
               case "ClientID" -> clientId = Integer.parseInt(value);
               case "AccessLevel" -> accessLevel = Integer.parseInt(value);
               default -> Log.global.warning("Unknown parameter: " + name + ":" + value);
            }
         }
      }
      return new ConnectResponse(header, request, msgControl, msgResponse, resultCode, clientId, accessLevel);
   }

   public boolean isConnectionOK() {
      return S_OK.equals(resultCode);
   }
}
