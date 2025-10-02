package no.imr.korona.ekremote;

import com.google.common.base.Splitter;
import no.imr.korona.ekremote.requests.parameter.GetParameterRequest;
import no.imr.korona.ekremote.requests.parameter.ParameterServer;
import no.imr.korona.ekremote.requests.parameter.SetParameterRequest;
import no.imr.korona.ekremote.responses.ResponseException;

import java.util.List;
import java.util.concurrent.TimeoutException;

public final class EkRemoteUtils {
   private EkRemoteUtils() {
   }

   public static List<String> getChannelIds(EkConnectionManager ekConnectionManager) throws TimeoutException, ResponseException {
      String value = ekConnectionManager.sendRequest(new GetParameterRequest(ParameterServer.TransceiverMgr.Channels)).value();
      return Splitter.on(',').omitEmptyStrings().splitToList(value);
   }

   public static boolean getSaveRawData(EkConnectionManager ekConnectionManager) throws TimeoutException, ResponseException {
      String value = ekConnectionManager.sendRequest(new GetParameterRequest(ParameterServer.SounderStorageManager.SaveRawData)).value();
      return "1".equals(value);
   }

   public static void setSaveRawData(EkConnectionManager ekConnectionManager, boolean saveRawData) throws TimeoutException, ResponseException {
      String value = saveRawData ? "1" : "0";
      ekConnectionManager.sendRequest(new SetParameterRequest(ParameterServer.SounderStorageManager.SaveRawData, value));
   }

   public static boolean isPinging(EkConnectionManager ekConnectionManager) throws TimeoutException, ResponseException {
      int value = Integer.parseInt(ekConnectionManager.sendRequest(new GetParameterRequest(ParameterServer.OperationControl.OperationMode)).value());
      return (value & 1) == 1;
   }

   public static void setPinging(EkConnectionManager ekConnectionManager, boolean pinging) throws TimeoutException, ResponseException {
      int value = Integer.parseInt(ekConnectionManager.sendRequest(new GetParameterRequest(ParameterServer.OperationControl.OperationMode)).value());
      if (pinging) {
         value |= 1;
      } else {
         value &= ~1;
      }
      ekConnectionManager.sendRequest(new SetParameterRequest(ParameterServer.OperationControl.OperationMode, value));
   }
}
