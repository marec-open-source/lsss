package no.imr.korona.ekremote;

import no.imr.korona.ekremote.requests.parameter.GetParameterRequest;
import no.imr.korona.ekremote.requests.parameter.ParameterServer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("PMD.SystemPrintln")
final class EKConnectionMain {
   static final int PORT = 37655;

   private EKConnectionMain() {
   }

   public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
      EkConnection ekConnection = new EkConnection(InetAddress.getLocalHost(), PORT, System.out::println);
      System.out.println("ServerInfoResponse = " + ekConnection.getServerInfoResponse());
      String channelIds = ekConnection.sendRequest(new GetParameterRequest(ParameterServer.TransceiverMgr.Channels)).get().value();
      System.out.println("channelIds = " + channelIds);
      ekConnection.close();
   }
}
