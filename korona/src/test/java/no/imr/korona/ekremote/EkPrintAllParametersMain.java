package no.imr.korona.ekremote;

import no.imr.korona.ekremote.requests.parameter.GetParameterRequest;
import no.imr.korona.ekremote.requests.parameter.ParameterServer;
import no.imr.korona.ekremote.responses.ResponseException;
import no.imr.korona.ekremote.responses.parameter.GetParameterResponse;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("PMD.SystemPrintln")
final class EkPrintAllParametersMain {
   private EkPrintAllParametersMain() {
   }

   public static void main(String[] args) throws TimeoutException, ResponseException {
      EkConnectionManager ekConnectionManager = new EkConnectionManager("localhost", EKConnectionMain.PORT, Duration.ofSeconds(60), Duration.ofSeconds(0));

      for (Class<? extends ParameterServer.NormalParameter> clazz : ParameterServer.getAllParameterEnums()) {
         for (ParameterServer.NormalParameter parameter : clazz.getEnumConstants()) {
            String name = parameter.getName();
            printParameter(ekConnectionManager, name, parameter.getType());
         }
      }

      for (String channelId : EkRemoteUtils.getChannelIds(ekConnectionManager)) {
         System.out.println("---------------------------------------------");
         for (Class<? extends ParameterServer.PerChannelParameter> clazz : ParameterServer.getAllPerChannelParameterEnums()) {
            for (ParameterServer.PerChannelParameter parameter : clazz.getEnumConstants()) {
               String name = parameter.getName(channelId);
               printParameter(ekConnectionManager, name, parameter.getType());
            }
         }
      }

      ekConnectionManager.close();
   }

   private static void printParameter(EkConnectionManager ekConnectionManager, String name, String type) throws TimeoutException, ResponseException {
      GetParameterResponse response = ekConnectionManager.sendRequest(new GetParameterRequest(name));
      String text = name + " = " + response.value();
      System.out.println(text + " ".repeat(Math.max(1, 100 - text.length())) + "type = " + response.type());
      assert Objects.equals(type, response.type());
   }
}
