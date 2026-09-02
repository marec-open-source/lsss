package no.imr.korona.ekremote;

import no.imr.korona.ekremote.requests.ClientInfo;
import no.imr.korona.ekremote.requests.MessageRequest;
import no.imr.korona.ekremote.requests.SequenceInfo;
import no.imr.korona.ekremote.requests.SimpleRequests;
import no.imr.korona.ekremote.requests.XmlRequest;
import no.imr.korona.ekremote.responses.AliveResponse;
import no.imr.korona.ekremote.responses.ConnectResponse;
import no.imr.korona.ekremote.responses.Fault;
import no.imr.korona.ekremote.responses.MessageResponse;
import no.imr.korona.ekremote.responses.RequestResponse;
import no.imr.korona.ekremote.responses.Response;
import no.imr.korona.ekremote.responses.ResponseException;
import no.imr.korona.ekremote.responses.ResponseFactory;
import no.imr.korona.ekremote.responses.RetransmitResponse;
import no.imr.korona.ekremote.responses.ServerInfoResponse;
import no.imr.korona.ekremote.responses.UnknownResponse;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.concurrent.SerialExecutor;
import no.imr.tools.listening.Listeners;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

final class EkConnection {
   private static final int SO_TIMEOUT = 5000;
   private static final int ALIVE_SEND_INTERVAL = 1000;
   private static final long ALIVE_RECEIVE_THRESHOLD = 10000;

   private final InetAddress inetAddress;
   private final Consumer<String> failureListener;

   private final ServerInfoResponse serverInfoResponse;
   private final DatagramSocket commandSocket;
   private final ConnectResponse connectionResponse;
   private final ScheduledFuture<?> sendAliveFuture;

   private final Map<Integer, RequestFutureContainer<?>> requestFutureContainers = new ConcurrentHashMap<>();

   private final Object sendLock = new Object();
   private int sequenceNumber = 1;
   private int requestId = 1;

   private Instant incomingAliveTime = Instant.now();
   private int incomingSequenceNumber = 1;

   EkConnection(InetAddress inetAddress, int port, Consumer<String> failureListener) throws IOException {
      this.inetAddress = inetAddress;
      this.failureListener = failureListener;
      serverInfoResponse = requestServerInfo(inetAddress, port);
      commandSocket = new DatagramSocket();
      try {
         commandSocket.connect(inetAddress, serverInfoResponse.commandPort());
         connectionResponse = requestConnectionResponse();
      } catch (IOException e) {
         commandSocket.close();
         throw e;
      }
      sendAliveFuture = Exec.scheduleWithFixedDelay(this::sendAliveMessage, ALIVE_SEND_INTERVAL, TimeUnit.MILLISECONDS);
      DatagramSocketListener.start(commandSocket,
            Listeners.inExecutor(new SerialExecutor(Exec.FORK_JOIN_POOL), this::handleResponse),
            exception -> failureListener.accept("Command socket error: " + exception));
   }

   private static ServerInfoResponse requestServerInfo(InetAddress inetAddress, int port) throws IOException {
      try (DatagramSocket socket = new DatagramSocket()) {
         socket.setSoTimeout(SO_TIMEOUT);
         byte[] request = SimpleRequests.getServerInfoRequest();
         DatagramPacket packet = new DatagramPacket(request, request.length, inetAddress, port);
         socket.send(packet);

         Response response = receiveResponse(socket);
         if (response instanceof ServerInfoResponse serverInfoResponse) {
            return serverInfoResponse;
         } else {
            throw new IOException("Did not get server info response: " + response.header());
         }
      }
   }

   private ConnectResponse requestConnectionResponse() throws IOException {
      sendOnCommandSocket(SimpleRequests.getConnectRequest());
      Response response = receiveResponse(commandSocket);
      if (response instanceof ConnectResponse connectResponse) {
         if (!connectResponse.isConnectionOK()) {
            throw new IOException("Failed to connect to " + serverInfoResponse.applicationName() + ": " + connectResponse.msgResponse());
         }
         return connectResponse;
      } else {
         throw new IOException("Did not get connection response: " + response.header());
      }
   }

   ServerInfoResponse getServerInfoResponse() {
      return serverInfoResponse;
   }

   private void sendOnCommandSocket(byte[] request) throws IOException {
      DatagramPacket datagramPacket = new DatagramPacket(request, request.length, inetAddress, serverInfoResponse.commandPort());
      commandSocket.send(datagramPacket);
   }

   private static Response receiveResponse(DatagramSocket datagramSocket) throws IOException {
      byte[] bytes = new byte[ResponseFactory.MAX_BYTE_SIZE];
      DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
      datagramSocket.receive(packet);
      return ResponseFactory.parse(bytes);
   }

   private void handleResponse(byte[] bytes) {
      Response response;
      try {
         response = ResponseFactory.parse(bytes);
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error creating response: " + new String(bytes, Utils.ISO_8859_1), e);
         return;
      }

      switch (response) {
         case AliveResponse aliveResponse -> handleAliveResponse(aliveResponse);
         case RequestResponse requestResponse -> handleRequestResponse(requestResponse);
         case RetransmitResponse retransmitResponse -> handleRetransmitResponse(retransmitResponse);
         case UnknownResponse _ -> Log.global.info("Unknown response: " + new String(bytes, Utils.ISO_8859_1));
         default -> Log.global.info("Unexpected response: " + response);
      }
   }

   private void handleAliveResponse(AliveResponse response) {
      incomingAliveTime = Instant.now();
      if (incomingSequenceNumber != response.sequenceNumber()) {
         // todo: Request retransmit
         failureListener.accept("Sequence number mismatch: Expected: " + incomingSequenceNumber + ", got: " + response);
      }
   }

   private void handleRequestResponse(RequestResponse response) {
      ClientInfo clientInfo = response.clientInfo();
      if (clientInfo.clientId() != connectionResponse.clientId()) {
         return;
      }
      if (incomingSequenceNumber != response.sequenceInfo().sequenceNumber()) {
         // todo: Request retransmit
         failureListener.accept("Sequence number mismatch: Expected: " + incomingSequenceNumber + ", got: " + response);
         return;
      }
      incomingSequenceNumber = response.sequenceInfo().sequenceNumber() + 1;
      RequestFutureContainer<?> requestFutureContainer = requestFutureContainers.remove(clientInfo.requestId());
      if (requestFutureContainer == null) {
         Log.global.warning("No unhandled future for request " + clientInfo.requestId());
         return;
      }
      requestFutureContainer.setRequestResponse(response);
   }

   private void handleRetransmitResponse(RetransmitResponse response) {
      // todo: retransmit
      failureListener.accept("Got retransmit response: " + response);
   }

   private void sendAliveMessage() {
      if (incomingAliveTime.until(Instant.now(), ChronoUnit.MILLIS) > ALIVE_RECEIVE_THRESHOLD) {
         failureListener.accept("Not getting alive reports");
         return;
      }

      try {
         synchronized (sendLock) {
            sendOnCommandSocket(SimpleRequests.getAliveRequest(connectionResponse.clientId(), sequenceNumber));
         }
      } catch (IOException e) {
         failureListener.accept("Error sending alive request: " + e);
      }
   }

   <T extends MessageResponse> Future<T> sendRequest(MessageRequest<T> messageRequest) throws IOException {
      synchronized (sendLock) {
         SequenceInfo sequenceInfo = new SequenceInfo(sequenceNumber, 1, 1);
         ClientInfo clientInfo = new ClientInfo(connectionResponse.clientId(), requestId);
         XmlRequest request = new XmlRequest(sequenceInfo, clientInfo, messageRequest);
         RequestFutureContainer<T> requestFutureContainer = new RequestFutureContainer<>(messageRequest);
         requestFutureContainers.put(requestId, requestFutureContainer);
         sequenceNumber++;
         requestId++;
         sendOnCommandSocket(request.getBytes());
         return requestFutureContainer.future;
      }
   }

   void close() {
      if (commandSocket.isClosed()) {
         return;
      }
      sendAliveFuture.cancel(false);
      Utils.awaitFuture(sendAliveFuture);
      try {
         sendOnCommandSocket(SimpleRequests.getDisconnectRequest());
      } catch (IOException e) {
         Log.global.warning("Error sending disconnect request: " + e);
      }
      commandSocket.close();
      for (RequestFutureContainer<?> requestFutureContainer : requestFutureContainers.values()) {
         requestFutureContainer.future.cancel(true);
      }
      requestFutureContainers.clear();
   }

   private static final class RequestFutureContainer<T extends MessageResponse> {
      private final MessageRequest<T> messageRequest;
      private final FutureTask<T> future = new FutureTask<>(this::parseResponse);
      private @Nullable RequestResponse requestResponse;

      private RequestFutureContainer(MessageRequest<T> messageRequest) {
         this.messageRequest = messageRequest;
      }

      private void setRequestResponse(RequestResponse requestResponse) {
         this.requestResponse = requestResponse;
         future.run();
      }

      private T parseResponse() throws ResponseException {
         assert requestResponse != null;
         Fault fault = requestResponse.fault();
         if (fault.isError()) {
            throw new ResponseException("Response with fault: " + fault);
         }

         Element responseElement = requestResponse.responseXml().element(messageRequest.getResponseXmlElementName());
         if (responseElement == null) {
            throw new ResponseException("Response XML does not contain element " + messageRequest.getResponseXmlElementName()
                  + ": " + XmlUtils.toCompactString(requestResponse.responseXml()));
         }

         try {
            return messageRequest.parseResponse(responseElement);
         } catch (Exception e) {
            throw new ResponseException(e);
         }
      }
   }
}
