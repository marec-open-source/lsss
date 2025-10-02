package no.imr.korona.ekremote.requests;

import no.imr.tools.Utils;

public final class SimpleRequests {
   private static final String SERVER_INFO = "RSI\0";
   private static final String CONNECT = "CON\0Name:Simrad;Password:\0";
   private static final String DISCONNECT = "DIS\0Name:Simrad;Password:\0";
   private static final String ALIVE_HEADER = "ALI\0";

   private SimpleRequests() {
   }

   public static byte[] getServerInfoRequest() {
      return toBytes(SERVER_INFO);
   }

   public static byte[] getConnectRequest() {
      return toBytes(CONNECT);
   }

   public static byte[] getDisconnectRequest() {
      return toBytes(DISCONNECT);
   }

   public static byte[] getAliveRequest(int clientID, int sequenceNumber) {
      String aliveRequest = ALIVE_HEADER + "ClientID:" + clientID + ",SeqNo:" + sequenceNumber + "\0";
      return toBytes(aliveRequest);
   }

   private static byte[] toBytes(String text) {
      return text.getBytes(Utils.ISO_8859_1);
   }
}
