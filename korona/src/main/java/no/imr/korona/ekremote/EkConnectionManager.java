package no.imr.korona.ekremote;

import no.imr.korona.ekremote.requests.MessageRequest;
import no.imr.korona.ekremote.responses.MessageResponse;
import no.imr.korona.ekremote.responses.ResponseException;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class EkConnectionManager {
   private static final Duration RETRY_INTERVAL = Duration.ofSeconds(1);

   private final String host;
   private final int port;
   private final Duration timeout;
   private final Duration waitAfterConnection;
   private final Consumer<String> failureListener = this::handleFailure;
   private @Nullable EkConnection ekConnection;

   public EkConnectionManager(String host, int port, Duration timeout, Duration waitAfterConnection) {
      this.host = host;
      this.port = port;
      this.timeout = timeout;
      this.waitAfterConnection = waitAfterConnection;
      connect();
   }

   private void connect() {
      Instant timeoutTime = Instant.now().plus(timeout);
      boolean hasLoggedError = false;
      while (Instant.now().isBefore(timeoutTime)) {
         try {
            InetAddress inetAddress = InetAddress.getByName(host);
            if (inetAddress.isLoopbackAddress()) {
               // todo: WHY? DatagramSocket.receive times out with InetAddress.getByName("localhost");
               inetAddress = InetAddress.getLocalHost();
            }
            ekConnection = new EkConnection(inetAddress, port, failureListener);
            Log.global.info("Connected to echosounder " + host + ":" + port + ". Will now wait for " + waitAfterConnection);
            Thread.sleep(waitAfterConnection);
            return;
         } catch (Exception e) {
            if (!hasLoggedError) {
               hasLoggedError = true;
               Log.global.warning("Error connecting to echosounder " + host + ":" + port + ": " + e);
            }
            Utils.sleep(RETRY_INTERVAL);
         }
      }
      Log.global.warning("Timeout connecting to echosounder " + host + ":" + port);
   }

   private void handleFailure(String message) {
      Log.global.warning("Restarting connection due to failure: " + message);
      reconnect();
   }

   private void reconnect() {
      close();
      connect();
   }

   public void close() {
      if (ekConnection != null) {
         ekConnection.close();
      }
      ekConnection = null;
   }

   public <T extends MessageResponse> T sendRequest(MessageRequest<T> request) throws TimeoutException, ResponseException {
      Instant timeoutTime = Instant.now().plus(timeout);
      boolean hasLoggedError = false;
      while (true) {
         long remainingMillis = Instant.now().until(timeoutTime, ChronoUnit.MILLIS);
         if (remainingMillis <= 0) {
            throw new TimeoutException("Time out");
         }
         if (ekConnection == null) {
            Utils.sleep(RETRY_INTERVAL);
            continue;
         }
         try {
            Future<T> future = ekConnection.sendRequest(request);
            try {
               return future.get(remainingMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | CancellationException _) {
               // try again
            } catch (ExecutionException e) {
               Throwable cause = e.getCause();
               if (cause instanceof ResponseException responseException) {
                  throw responseException;
               }
               Log.global.log(Level.WARNING, "Error receiving response for " + request, e);
               // try again
            }
         } catch (IOException e) {
            if (!hasLoggedError) {
               hasLoggedError = true;
               Log.global.log(Level.WARNING, "Error sending request: " + request, e);
            }
         }
         Utils.sleep(RETRY_INTERVAL);
         reconnect();
      }
   }
}
