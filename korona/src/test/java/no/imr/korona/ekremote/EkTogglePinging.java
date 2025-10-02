package no.imr.korona.ekremote;

import no.imr.korona.ekremote.responses.ResponseException;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("PMD.SystemPrintln")
final class EkTogglePinging {
   private EkTogglePinging() {
   }

   public static void main(String[] args) throws TimeoutException, ResponseException {
      EkConnectionManager ekConnectionManager = new EkConnectionManager("localhost", EKConnectionMain.PORT, Duration.ofSeconds(60), Duration.ofSeconds(0));

      boolean pinging = EkRemoteUtils.isPinging(ekConnectionManager);
      pinging = !pinging;
      System.out.println("Switching pinging to " + pinging);
      EkRemoteUtils.setPinging(ekConnectionManager, pinging);

      ekConnectionManager.close();
   }
}
