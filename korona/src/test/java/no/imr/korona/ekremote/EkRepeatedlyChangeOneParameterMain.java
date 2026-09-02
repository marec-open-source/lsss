package no.imr.korona.ekremote;

import no.imr.korona.ekremote.requests.parameter.GetParameterRequest;
import no.imr.korona.ekremote.requests.parameter.ParameterServer;
import no.imr.korona.ekremote.requests.parameter.SetParameterRequest;
import no.imr.korona.ekremote.responses.ResponseException;
import no.imr.korona.ekremote.responses.parameter.GetParameterResponse;
import no.imr.tools.Utils;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("PMD.SystemPrintln")
final class EkRepeatedlyChangeOneParameterMain {
   private EkRepeatedlyChangeOneParameterMain() {
   }

   static void main() throws TimeoutException, ResponseException {
      EkConnectionManager ekConnectionManager = new EkConnectionManager("localhost", EKConnectionMain.PORT, Duration.ofSeconds(60), Duration.ofSeconds(0));

      for (int i = 0; i < 1000; i++) {
         ParameterServer.NormalParameter parameter = ParameterServer.RemoteCommandDispatcher.ClientTimeoutLimit;
         ekConnectionManager.sendRequest(new SetParameterRequest(parameter, 30 + i % 10));
         GetParameterResponse getParameterResponse = ekConnectionManager.sendRequest(new GetParameterRequest(parameter));
         String value = getParameterResponse.value();
         System.out.println(i + ": " + parameter.getName() + " = " + value);
         Utils.sleep(Duration.ofMillis(100));
      }

      ekConnectionManager.close();
   }
}
