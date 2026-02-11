package no.imr.korona.ekremote;

import no.imr.korona.ekremote.requests.parameter.GetParameterRequest;
import no.imr.korona.ekremote.requests.parameter.ParameterServer;
import no.imr.korona.ekremote.requests.parameter.SetParameterRequest;
import no.imr.korona.ekremote.responses.parameter.GetParameterResponse;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("PMD.SystemPrintln")
final class EkTestMain {
   private EkTestMain() {
   }

   static void main() throws IOException, ExecutionException, InterruptedException {
      EkConnection ekConnection = new EkConnection(InetAddress.getLocalHost(), EKConnectionMain.PORT, System.out::println);

      //ekConnection.sendRequest(new GetParameterRequest(ParameterServer.AcousticDeviceSynchroniser.Interval)).get().getValue();
      //ekConnection.sendRequest(new GetParameterRequest(ParameterServer.SounderStorageManager.SaveRawData)).get().getValue();

      //ekConnection.sendRequest(new SetParameterRequest(ParameterServer.AcousticDeviceSynchroniser.Interval, 1000)).get();
      //GetParameterResponse response = ekConnection.sendRequest(new GetParameterRequest(ParameterServer.AcousticDeviceSynchroniser.Interval)).get();
      //System.out.println(response.getValue());

      //GetParameterResponse response = ekConnection.sendRequest(new GetParameterRequest(ParameterServer.OperationControl.OperationMode)).get();
      //System.out.println(response.getValue());

      ekConnection.sendRequest(new SetParameterRequest(ParameterServer.TransceiverMgr.VesselDistance.getName(), "1.1", "3")).get();
      print(ekConnection, ParameterServer.TransceiverMgr.VesselDistance);
      //ekConnection.sendRequest(new SetParameterRequest(ParameterServer.OwnShip.EnvironmentData.SoundVelocity.getName(), "1491", "5")).get();
      //print(ekConnection, ParameterServer.OwnShip.EnvironmentData.SoundVelocity);

/*
      ekConnection.sendRequest(new SetParameterRequest(ParameterServer.SounderStorageManager.SaveRawData, 1)).get();
      ekConnection.sendRequest(new SetParameterRequest(ParameterServer.SounderStorageManager.SaveIndexFile, 1)).get();
      ekConnection.sendRequest(new SetParameterRequest(ParameterServer.SounderStorageManager.SaveBottomDepthFile, 1)).get();
      ekConnection.sendRequest(new SetParameterRequest(ParameterServer.SounderStorageManager.SampleRange, 134)).get();

      print(ekConnection, ParameterServer.SounderStorageManager.SaveIndexFile);
      print(ekConnection, ParameterServer.SounderStorageManager.SaveRawData);
      print(ekConnection, ParameterServer.SounderStorageManager.SaveBottomDepthFile);
      print(ekConnection, ParameterServer.SounderStorageManager.SampleRange);
*/

      ekConnection.close();
   }

   private static void print(EkConnection ekConnection, ParameterServer.NormalParameter parameter) throws InterruptedException, ExecutionException, IOException {
      GetParameterResponse response = ekConnection.sendRequest(new GetParameterRequest(parameter)).get();
      System.out.println(parameter.getName() + " = " + response.value());
   }
}
