package no.imr.korona.ekremote;

import no.imr.korona.ekremote.responses.ResponseException;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("PMD.SystemPrintln")
final class EkToggleSaveRawData {
   private EkToggleSaveRawData() {
   }

   static void main() throws TimeoutException, ResponseException {
      EkConnectionManager ekConnectionManager = new EkConnectionManager("localhost", EKConnectionMain.PORT, Duration.ofSeconds(60), Duration.ofSeconds(0));

      boolean saveRawData = EkRemoteUtils.getSaveRawData(ekConnectionManager);
      saveRawData = !saveRawData;
      System.out.println("Switching save raw data to " + saveRawData);
      EkRemoteUtils.setSaveRawData(ekConnectionManager, saveRawData);

      ekConnectionManager.close();
   }
}
